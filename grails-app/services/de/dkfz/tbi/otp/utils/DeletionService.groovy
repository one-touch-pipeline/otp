/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.utils

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.administration.ProjectInfo
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.egaSubmission.EgaSubmission
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold

import java.nio.file.Path

import static org.springframework.util.Assert.notNull

@SuppressWarnings('Println') //This class is written for scripts, so it needs the output in stdout
@Transactional
class DeletionService {

    CommentService commentService
    FastqcDataFilesService fastqcDataFilesService
    LsdfFilesService lsdfFilesService
    DataProcessingFilesService dataProcessingFilesService
    SeqTrackService seqTrackService
    AnalysisDeletionService analysisDeletionService
    FileService fileService
    DataSwapService dataSwapService
    RunService runService
    ConfigService configService

    void assertNoEgaSubmissionsForProject(Project project) {
        assert !EgaSubmission.findAllByProject(project): "There are Ega Submissions connected to this Project, thus it can not be deleted"
    }

    void deleteProjectContent(Project project) {
        assertNoEgaSubmissionsForProject(project)

        // Delete individuals for a project
        Individual.findAllByProject(project).each { individual ->
            deleteIndividual(individual, false)
        }

        // There are files which are not connected to a seqTrack -> they have to be deleted, too
        DataFile.findAllByProject(project).each { dataFile ->
            deleteDataFile(dataFile)
        }
    }

    void deleteProject(Project project) {
        deleteProjectContent(project)
        deleteProjectDependencies(project)
        project.delete(flush: true)
    }

    void deleteProjectDependencies(Project project) {
        // Deletes the connection of the project to the reference genome
        ReferenceGenomeProjectSeqType.findAllByProject(project)*.delete(flush: true)

        MergingCriteria.findAllByProject(project)*.delete(flush: true)

        ProcessingThresholds.findAllByProject(project)*.delete(flush: true)

        SampleTypePerProject.findAllByProject(project)*.delete(flush: true)

        // Deletes the ProcessingOptions of the project
        ProcessingOption.findAllByProject(project)*.delete(flush: true)

        List configPerProjectAndSeqTypes = ConfigPerProjectAndSeqType.findAllByProject(project)
        configPerProjectAndSeqTypes*.previousConfig = null
        configPerProjectAndSeqTypes*.delete(flush: true)

        UserProjectRole.findAllByProject(project)*.delete(flush: true)
        QcThreshold.findAllByProject(project)*.delete(flush: true)
        ProjectInfo.findAllByProject(project)*.delete(flush: true)
    }

    List<String> deleteIndividual(String pid, boolean check = true) {
        Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(pid), "No individual could be found for PID ${pid}")
        return deleteIndividual(individual, check)
    }

    List<String> deleteIndividual(Individual individual, boolean check = true) {
        StringBuilder deletionScript = new StringBuilder()
        StringBuilder deletionScriptOtherUser = new StringBuilder()

        List<Sample> samples = Sample.findAllByIndividual(individual)

        List<SeqType> seqTypes = []

        samples.each { Sample sample ->
            List<SeqTrack> seqTracks = SeqTrack.findAllBySample(sample)

            seqTracks.each { SeqTrack seqTrack ->
                seqTrack.dataFiles.each { DataFile dataFile ->
                    String filePath = lsdfFilesService.getFileFinalPath(dataFile)
                    if (filePath) {
                        deletionScript << "rm -rf ${new File(filePath).absolutePath}\n"
                    }
                }
                Map<String, List<File>> seqTrackDirsToDelete = deleteSeqTrack(seqTrack, check)

                seqTrackDirsToDelete.get("dirsToDelete").flatten().findAll().each {
                    deletionScript << "rm -rf ${it.absolutePath}\n"
                }
                seqTrackDirsToDelete.get("dirsToDeleteWithOtherUser").flatten().findAll().each {
                    deletionScriptOtherUser << "rm -rf ${it.absolutePath}\n"
                }
                seqTypes.add(seqTrack.seqType)

                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(seqTrack.id.toString(), seqTrack.class.name))
            }

            SeqScan.findAllBySample(sample)*.delete(flush: true)
            SampleIdentifier.findAllBySample(sample)*.delete(flush: true)
            sample.delete(flush: true)
        }

        seqTypes.unique().each { SeqType seqType ->
            deletionScript << "rm -rf ${individual.getViewByPidPath(seqType).absoluteDataManagementPath}\n"
        }

        deleteClusterJobs(ClusterJob.findAllByIndividual(individual))
        individual.delete(flush: true)

        return [deletionScript.toString(), deletionScriptOtherUser.toString()]
    }

    void deleteProcessParameters(List<ProcessParameter> processParameters) {
        processParameters.each {
            deleteProcessParameter(it)
        }
    }

    private void deleteProcessParameter(ProcessParameter processParameter) {
        if (processParameter) {
            Process process = processParameter.process
            processParameter.delete(flush: true)
            deleteProcess(process)
        }
    }

    private void deleteProcess(Process process) {
        assert process.finished : "process with id ${process.id} not finished"
        Process.findAllByRestarted(process).each {
            deleteProcess(it)
        }
        deleteProcessingSteps(ProcessingStep.findAllByProcess(process))
        process.delete(flush: true)
    }

    private void deleteProcessingSteps(List<ProcessingStep> processingSteps) {
        processingSteps*.next = null
        processingSteps.sort { -it.id }
        processingSteps.each {
            deleteProcessingStep(it)
        }
    }

    private void deleteProcessingStep(ProcessingStep processingStep) {
        deleteClusterJobs(ClusterJob.findAllByProcessingStep(processingStep))
        deleteProcessingStepUpdates(ProcessingStepUpdate.findAllByProcessingStep(processingStep))
        processingStep.delete(flush: true)
    }

    private void deleteClusterJobs(List<ClusterJob> clusterJobs) {
        clusterJobs*.dependencies = [] as Set
        clusterJobs.each {
            deleteClusterJob(it)
        }
    }

    private void deleteClusterJob(ClusterJob clusterJob) {
        clusterJob.delete(flush: true)
    }

    private void deleteProcessingStepUpdates(List<ProcessingStepUpdate> processingStepUpdates) {
        processingStepUpdates*.previous = null
        processingStepUpdates.each {
            deleteProcessingStepUpdate(it)
        }
    }

    private void deleteProcessingStepUpdate(ProcessingStepUpdate processingStepUpdate) {
        ProcessingError error = processingStepUpdate.error
        if (error) {
            processingStepUpdate.error = null
            deleteProcessingError(error)
        }
        processingStepUpdate.delete(flush: true)
    }

    private void deleteProcessingError(ProcessingError processingError) {
        processingError.delete(flush: true)
    }

    /**
     * function to delete one sample inclusive all dependencies:
     * - seqTracks
     * - dataFiles
     * - fastqcProcessedFiles and the related objects
     * - metaDataEntries
     * - mergingAssignment
     * - seqTracks
     * - seqscan
     * - sample identifiers
     *
     * TODO: test this method
     * Attention: It is assumed that only fastq files exists for this sample.
     * Attention: It is assumed no alignment is done yet
     *
     * @param projectName the name of the project, the patient is connected with
     * @param pid the name of the individual the sample belongs to
     * @param sampleTypeName the name of the sample type the sample belongs to should be deleted
     * @param dataFileList A list of the file name of the datafiles to be deleted
     */
    /* is currently not used anywhere, the decision if this will be delete entirely will be taken in another issue
    void deleteSample(String projectName, String pid, String sampleTypeName, List<String> dataFileList, StringBuilder log) {
        log << "\n\ndelete ${pid} ${sampleTypeName} of ${projectName}"

        notNull(projectName, "parameter projectName may not be null")
        notNull(pid, "parameter pid may not be null")
        notNull(sampleTypeName, "parameter sampleTypeName may not be null")

        Project project = Project.findByName(projectName)
        notNull(project, "old project ${projectName} not found")

        Individual individual = Individual.findByPid(pid)
        notNull(individual, "pid ${pid} not found")
        isTrue(individual.project == project, "given project and project of individual are not the same ")

        SampleType sampleType = SampleType.findByName(sampleTypeName)
        notNull(sampleType, "sample type ${sampleTypeName} not found")

        Sample sample = dataSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, log)

        List<SeqTrack> seqTracks = dataSwapService.getAndShowSeqTracksForSample(sample, log)
        assert !seqTracks || !AlignmentPass.findBySeqTrackInList(seqTracks)

        dataSwapService.throwExceptionInCaseOfExternalMergedBamFileIsAttached(seqTracks)
        dataSwapService.throwExceptionInCaseOfSeqTracksAreOnlyLinked(seqTracks)

        List<DataFile> dataFiles = seqTracks ? DataFile.findAllBySeqTrackInList(seqTracks) : []
        log << "\n  dataFiles (${dataFiles.size()}):"
        dataFiles.each { log << "\n    - ${it}" }
        notEmpty(dataFiles, " no datafiles found for ${sample}")
        isTrue(dataFiles.size() == dataFileList.size(), "size of dataFiles (${dataFiles.size()}) and dataFileList (${dataFileList.size()}) not match")
        dataFiles.each {
            isTrue(dataFileList.contains(it.fileName), "${it.fileName} missed in list")
        }

        // validating ends here, now the changing are started

        //file system changes are already done, so they do not need to be done here
        dataFiles.each {
            //delete first fastqc stuff
            List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllByDataFile(it)

            fastqcProcessedFiles*.delete(flush: true)

            List<MetaDataEntry> metaDataEntries = MetaDataEntry.findAllByDataFile(it)
            metaDataEntries*.delete(flush: true)

            it.delete(flush: true)
            log << "\n    deleted datafile ${it} inclusive fastqc and metadataentries"
        }

        if (seqTracks) {
            MergingAssignment.findAllBySeqTrackInList(seqTracks)*.delete(flush: true)
        }

        seqTracks.each {
            it.delete(flush: true)
            log << "\n    deleted seqtrack ${it}"
        }

        SeqScan.findAllBySample(sample)*.delete(flush: true)
        SampleIdentifier.findAllBySample(sample)*.delete(flush: true)

        sample.delete(flush: true)
        log << "\n    deleted sample ${sample}"
    }*/

    /**
     * Removes all fastQC information about the dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteFastQCInformationFromDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFile is null")
        List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllByDataFile(dataFile)

        fastqcProcessedFiles*.delete(flush: true)
    }

    /**
     * Removes all metadata-entries, which belong to the dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteMetaDataEntryForDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFiles is null")
        MetaDataEntry.findAllByDataFile(dataFile)*.delete(flush: true)
    }

    /**
     * Removes all information about the consistency checks of the input dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteConsistencyStatusInformationForDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFiles is null")
        ConsistencyStatus.findAllByDataFile(dataFile)*.delete(flush: true)
    }

    /**
     * Removes all QA-Information & the MarkDuplicate-metrics for an AbstractBamFile.
     * As input the AbstractBamFile is chosen, so that the method can be used for ProcessedBamFiles and ProcessedMergedBamFiles.
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteQualityAssessmentInfoForAbstractBamFile(AbstractBamFile abstractBamFile) {
        notNull(abstractBamFile, "The input AbstractBamFile is null")
        if (abstractBamFile instanceof ProcessedBamFile) {
            List<QualityAssessmentPass> qualityAssessmentPasses = QualityAssessmentPass.findAllByProcessedBamFile(abstractBamFile)

            if (qualityAssessmentPasses) {
                ChromosomeQualityAssessment.findAllByQualityAssessmentPassInList(qualityAssessmentPasses)*.delete(flush: true)
                OverallQualityAssessment.findAllByQualityAssessmentPassInList(qualityAssessmentPasses)*.delete(flush: true)
            }

            qualityAssessmentPasses.each {
                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(it.id.toString(), it.class.name))
                it.delete(flush: true)
            }
        } else if (abstractBamFile instanceof ProcessedMergedBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(abstractBamFile)

            if (qualityAssessmentMergedPasses) {
                ChromosomeQualityAssessmentMerged.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete(flush: true)
                OverallQualityAssessmentMerged.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete(flush: true)
            }
            qualityAssessmentMergedPasses.each {
                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(it.id.toString(), it.class.name))
                it.delete(flush: true)
            }
        } else if (abstractBamFile instanceof RoddyBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(abstractBamFile)
            if (qualityAssessmentMergedPasses) {
                RoddyQualityAssessment.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete(flush: true)
            }
            qualityAssessmentMergedPasses*.delete(flush: true)
        } else if (abstractBamFile instanceof SingleCellBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(abstractBamFile)
            if (qualityAssessmentMergedPasses) {
                CellRangerQualityAssessment.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete()
            }
            qualityAssessmentMergedPasses*.delete()
        } else {
            throw new RuntimeException("This BamFile type " + abstractBamFile + " is not supported")
        }

        PicardMarkDuplicatesMetrics.findAllByAbstractBamFile(abstractBamFile)*.delete(flush: true)
    }

    /**
     * Delete merging related database entries, based on the mergingSetAssignments
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    Map<String, List<File>> deleteMergingRelatedConnectionsOfBamFile(ProcessedBamFile processedBamFile) {
        notNull(processedBamFile, "The input processedBamFile is null in method deleteMergingRelatedConnectionsOfBamFile")
        List<File> dirsToDelete = []
        List<File> dirsToDeleteWithOtherUser = []

        List<MergingSetAssignment> mergingSetAssignments = MergingSetAssignment.findAllByBamFile(processedBamFile)
        List<MergingSet> mergingSets = mergingSetAssignments*.mergingSet
        List<MergingWorkPackage> mergingWorkPackages = mergingSets*.mergingWorkPackage

        if (mergingWorkPackages.empty) {
            println "there is no merging for processedBamFile " + processedBamFile
        } else if (mergingWorkPackages.unique().size() > 1) {
            throw new RuntimeException("There is not one unique mergingWorkPackage for ProcessedBamFile " + processedBamFile)
        } else {
            MergingWorkPackage mergingWorkPackage = mergingWorkPackages.first()
            List<MergingPass> mergingPasses = mergingSets ? MergingPass.findAllByMergingSetInList(mergingSets).unique() : []
            List<ProcessedMergedBamFile> processedMergedBamFiles = mergingPasses ? ProcessedMergedBamFile.findAllByMergingPassInList(mergingPasses) : []

            mergingSetAssignments*.delete(flush: true)

            if (processedMergedBamFiles) {
                BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(processedMergedBamFiles, processedMergedBamFiles).each {
                    dirsToDeleteWithOtherUser << AnalysisDeletionService.deleteInstance(it)
                }
            }

            processedMergedBamFiles.each { ProcessedMergedBamFile processedMergedBamFile ->
                deleteQualityAssessmentInfoForAbstractBamFile(processedMergedBamFile)
                MergingSetAssignment.findAllByBamFile(processedMergedBamFile)*.delete(flush: true)

                dirsToDelete.addAll(analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(
                        SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage)))

                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(processedMergedBamFile.id.toString(), processedMergedBamFile.class.name))
                processedMergedBamFile.delete(flush: true)
            }

            mergingPasses.each {
                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(it.id.toString(), it.class.name))
                it.delete(flush: true)
            }

            mergingSets.each { MergingSet mergingSet ->
                // The MergingSet can only be deleted if all corresponding AbstractBamFiles are removed already
                if (!MergingSetAssignment.findByMergingSet(mergingSet)) {
                    mergingSet.delete(flush: true)
                }
            }
            // The MerginWorkPackage can only be deleted if all corresponding MergingSets and AlignmentPasses are removed already
            if (!MergingSet.findByMergingWorkPackage(mergingWorkPackage) && !AlignmentPass.findByWorkPackage(mergingWorkPackage)) {
                mergingWorkPackage.delete(flush: true)
            }
        }
        return ["dirsToDelete": dirsToDelete,
                "dirsToDeleteWithOtherUser": dirsToDeleteWithOtherUser,]
    }

    /**
     * Deletes a dataFile and all corresponding information
     */
    List<File> deleteDataFile(DataFile dataFile) {
        notNull(dataFile, "The dataFile input of method deleteDataFile is null")

        String fileFinalPath = lsdfFilesService.getFileFinalPath(dataFile)
        List<File> dirs = [
                fileFinalPath,
                "${fileFinalPath}.md5sum",
                lsdfFilesService.getFileViewByPidPath(dataFile),
        ].collect { new File(it) }

        deleteFastQCInformationFromDataFile(dataFile)
        deleteMetaDataEntryForDataFile(dataFile)
        deleteConsistencyStatusInformationForDataFile(dataFile)
        dataFile.delete(flush: true)
        return dirs
    }

    /**
     * Deletes the datafiles, which represent external bam files, and the connection to the seqTrack (alignmentLog).
     *
     * There are not only fastq files, which are represented by a dataFile, but also bam files.
     * They are not directly connected to a seqTrack, but via an alignmentLog.
     */
    void deleteConnectionFromSeqTrackRepresentingABamFile(SeqTrack seqTrack) {
        notNull(seqTrack, "The input seqTrack of the method deleteConnectionFromSeqTrackRepresentingABamFile is null")

        AlignmentLog.findAllBySeqTrack(seqTrack).each { AlignmentLog alignmentLog ->
            DataFile.findAllByAlignmentLog(alignmentLog).each { deleteDataFile(it) }
            alignmentLog.delete(flush: true)
        }
    }

    /**
     * Delete all processing information and results in the DB which are connected to one SeqTrack
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * !! Be aware that the run information, alignmentLog information, mergingLog information and the seqTrack are not deleted.
     * !! If it is not needed to delete this information, this method can be used without pre-work.
     */
    Map<String, List<File>> deleteAllProcessingInformationAndResultOfOneSeqTrack(SeqTrack seqTrack, boolean enableChecks = true) {
        notNull(seqTrack, "The input seqTrack of the method deleteAllProcessingInformationAndResultOfOneSeqTrack is null")
        List<File> dirsToDelete = []
        List<File> dirsToDeleteWithOtherUser = []

        if (enableChecks) {
            dataSwapService.throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
            dataSwapService.throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
        }

        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)

        if (dataFiles) {
            ProcessedSaiFile.findAllByDataFileInList(dataFiles)*.delete(flush: true)
        }

        // for ProcessedMergedBamFiles
        AlignmentPass.findAllBySeqTrack(seqTrack).each { AlignmentPass alignmentPass ->
            MergingWorkPackage mergingWorkPackage = alignmentPass.workPackage
            mergingWorkPackage.bamFileInProjectFolder = null
            mergingWorkPackage.save(flush: true)
            ProcessedBamFile.findAllByAlignmentPass(alignmentPass).each { ProcessedBamFile processedBamFile ->
                deleteQualityAssessmentInfoForAbstractBamFile(processedBamFile)
                Map<String, List<File>> processingDirsToDelete = deleteMergingRelatedConnectionsOfBamFile(processedBamFile)
                dirsToDelete.addAll(processingDirsToDelete["dirsToDelete"])
                dirsToDeleteWithOtherUser.addAll(processingDirsToDelete["dirsToDeleteWithOtherUser"])
                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(processedBamFile.id.toString(), processedBamFile.class.name))
                processedBamFile.delete(flush: true)
            }
            alignmentPass.delete(flush: true)
            // The MerginWorkPackage can only be deleted if all corresponding MergingSets and AlignmentPasses are removed already
            if (!MergingSet.findByMergingWorkPackage(mergingWorkPackage) && !AlignmentPass.findByWorkPackage(mergingWorkPackage)) {
                analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(
                        SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
                mergingWorkPackage.delete(flush: true)
            }
        }

        // for RoddyBamFiles
        MergingWorkPackage mergingWorkPackage = null
        List<RoddyBamFile> bamFiles = RoddyBamFile.createCriteria().list {
            seqTracks {
                eq("id", seqTrack.id)
            }
            order("id", "desc")
        }

        if (bamFiles) {
            BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(bamFiles, bamFiles).each {
                dirsToDeleteWithOtherUser << AnalysisDeletionService.deleteInstance(it)
                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(it.id.toString(), it.class.name))
            }
        }

        bamFiles.each { RoddyBamFile bamFile ->
            mergingWorkPackage = bamFile.mergingWorkPackage
            mergingWorkPackage.bamFileInProjectFolder = null
            mergingWorkPackage.save(flush: true)
            deleteQualityAssessmentInfoForAbstractBamFile(bamFile)
            dirsToDelete << analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(
                    SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
            deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(bamFile.id.toString(), bamFile.class.name))
            dirsToDelete << bamFile.baseDirectory
            bamFile.baseBamFile = null
            bamFile.delete(flush: true)
            // The MerginWorkPackage can only be deleted if all corresponding RoddyBamFiles are removed already
            if (!RoddyBamFile.findByWorkPackage(mergingWorkPackage)) {
                dirsToDelete << analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(
                        SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
                mergingWorkPackage.delete(flush: true)
            }
        }
        // for SingleCellBamFiles
        List<SingleCellBamFile> singleCellBamFiles = SingleCellBamFile.createCriteria().list {
            seqTracks {
                eq("id", seqTrack.id)
            }
            order("id", "desc")
        }

        CellRangerMergingWorkPackage crmwp = null
        singleCellBamFiles.each { SingleCellBamFile bamFile ->
            crmwp = bamFile.mergingWorkPackage
            crmwp.bamFileInProjectFolder = null
            crmwp.save(flush: true)
            deleteQualityAssessmentInfoForAbstractBamFile(bamFile)
            deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(bamFile.id.toString(), bamFile.class.name))
            dirsToDelete << bamFile.workDirectory
            bamFile.delete(flush: true)
            if (!SingleCellBamFile.findByWorkPackage(crmwp)) {
                crmwp.delete(flush: true)
            }
        }

        return ["dirsToDelete": dirsToDelete,
                "dirsToDeleteWithOtherUser": dirsToDeleteWithOtherUser,]
    }

    /**
     * Deletes the SeqScan, corresponding information and its connections to the fastqfiles/seqTracks
     */
    void deleteSeqScanAndCorrespondingInformation(SeqScan seqScan) {
        notNull(seqScan, "The input seqScan of the method deleteSeqScanAndCorrespondingInformation is null")

        List<MergingLog> mergingLogs = MergingLog.findAllBySeqScan(seqScan)
        if (mergingLogs) {
            MergedAlignmentDataFile.findAllByMergingLogInList(mergingLogs)*.delete(flush: true)
        }
        mergingLogs*.delete(flush: true)
        seqScan.delete(flush: true)
    }

    /**
     * Deletes one SeqTrack from the DB
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * !! Be aware that the run information are not deleted.
     * There is always more than one seqTrack which belongs to one Run, which is why the run is not deleted.
     * !! If it is not needed to delete this information, this method can be used without pre-work.
     */
    Map<String, List<File>> deleteSeqTrack(SeqTrack seqTrack, boolean check = true) {
        notNull(seqTrack, "The input seqTrack of the method deleteSeqTrack is null")

        if (check) {
            dataSwapService.throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
            dataSwapService.throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
        }

        List<File> dirsToDelete = []
        Map<String, List<File>> seqTrackDelete = deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, check)
        dirsToDelete.addAll(seqTrackDelete["dirsToDelete"])
        deleteConnectionFromSeqTrackRepresentingABamFile(seqTrack)
        DataFile.findAllBySeqTrack(seqTrack).each { DataFile df ->
            dirsToDelete.addAll(deleteDataFile(df))
        }
        MergingAssignment.findAllBySeqTrack(seqTrack)*.delete(flush: true)

        seqTrack.delete(flush: true)

        if (runService.isRunEmpty(seqTrack.run)) {
            deleteEmptyRun(seqTrack.run)
        }

        return ["dirsToDelete": dirsToDelete, "dirsToDeleteWithOtherUser": seqTrackDelete["dirsToDeleteWithOtherUser"]]
    }

    void deleteEmptyRun(Run run) {
        assert run: "The input run of the method deleteRun is null"
        deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(run.id.toString(), Run.name))
        run.delete(flush: true)
    }

    /**
     * Deletes the given run with all connected data.
     * This includes:
     * - all referenced datafiles using deleteDataFile
     * - all seqtrack using deleteSeqTrack
     * - the run itself
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    List<File> deleteRun(Run run) {
        notNull(run, "The input run of the method deleteRun is null")

        List<File> dirsToDelete = []

        DataFile.findAllByRun(run).each {
            dirsToDelete.addAll(deleteDataFile(it))
        }

        dirsToDelete.addAll(SeqTrack.findAllByRun(run).collect {
            return deleteSeqTrack(it).get("dirsToDelete")
        }.flatten() as List<File>)

        if (Run.exists(run.id) && runService.isRunEmpty(run)) {
            deleteEmptyRun(run)
        }

        return dirsToDelete
    }

    /**
     * Deletes the run given by Name.
     * The method fetch the run for the name and call then deleteRun.
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * @see #deleteRun(de.dkfz.tbi.otp.ngsdata.Run)
     */
    List<File> deleteRunByName(String runName) {
        notNull(runName, "The input runName of the method deleteRunByName is null")
        Run run = Run.findByName(runName)
        notNull(run, "No run with name ${runName} could be found in the database")
        return deleteRun(run)
    }

    /**
     * Deletes all processed files after the fastqc step from the give project.
     *
     * For the cases where the fastq files are not available in the project folder it has to be checked if the fastq files are still available on midterm.
     * If this is the case the GPCF has to be informed that the must not delete these fastq files during the sample swap.
     * If ExternallyProcessedMergedBamFiles were imported for this project, an exception is thrown to give the opportunity for clarification.
     * If everything was clarified the method can be called with true for "everythingVerified" so that the mentioned checks won't be executed anymore.
     * If the fastq files are not available an error is thrown.
     * If withdrawn data should be ignored, set ignoreWithdrawn "true"
     *
     * If explicitSeqTracks is defined, the defined list of seqTracks will be querried
     *
     * Return a list containing the affected seqTracks
     */
    List<SeqTrack> deleteProcessingFilesOfProject(String projectName, Path scriptOutputDirectory, boolean everythingVerified = false,
                                                  boolean ignoreWithdrawn = false, List<SeqTrack> explicitSeqTracks = []) throws FileNotFoundException {
        Project project = Project.findByName(projectName)
        assert project : "Project does not exist"

        Set<String> dirsToDelete = [] as Set
        Set<String> externalMergedBamFolders = [] as Set

        StringBuilder output = new StringBuilder()

        List<DataFile> dataFiles

        if (explicitSeqTracks.empty) {
            dataFiles = DataFile.createCriteria().list {
                seqTrack {
                    sample {
                        individual {
                            eq('project', project)
                        }
                    }
                }
            }
        } else {
            assert CollectionUtils.exactlyOneElement(explicitSeqTracks*.project.unique()) == project
            dataFiles = explicitSeqTracks ? DataFile.findAllBySeqTrackInList(explicitSeqTracks) : []
        }

        output << "found ${dataFiles.size()} data files for this project\n\n"
        assert !dataFiles.empty : "There are no SeqTracks attached to this project ${projectName}"

        List<DataFile> withdrawnDataFiles = []
        List<DataFile> missingFiles = []
        List<String> filesToClarify = []

        boolean throwException = false

        dataFiles.each {
            if (new File(lsdfFilesService.getFileViewByPidPath(it)).exists()) {
                if (it.seqTrack.linkedExternally && !everythingVerified) {
                    filesToClarify << lsdfFilesService.getFileInitialPath(it)
                    throwException = true
                }
            } else {
                // withdrawn data must have no existing fastq files,
                // to distinguish between missing files and withdrawn files this gets queried
                // an error is thrown as long as ignoreWithdrawn is false
                if (it.fileWithdrawn) {
                    withdrawnDataFiles << it
                    if (!ignoreWithdrawn) {
                        throwException = true
                    }
                } else {
                    throwException = true
                    missingFiles << it
                }
            }
        }

        if (withdrawnDataFiles) {
            output << "The fastq files of the following ${withdrawnDataFiles.size()} data files are withdrawn: \n${withdrawnDataFiles.join("\n")}\n\n"
        }

        if (missingFiles) {
            output << "The fastq files of the following ${missingFiles.size()} data files are missing: \n${missingFiles.join("\n")}\n\n"
        }

        if (filesToClarify) {
            output << "Talk to the sequencing center not to remove the following ${filesToClarify.size()} fastq files until the realignment is finished:" +
                    "\n ${filesToClarify.join("\n")}\n\n"
        }

        dataFiles = dataFiles - withdrawnDataFiles
        List<SeqTrack> seqTracks = dataFiles*.seqTrack.unique()

        // in case there are no dataFiles/seqTracks left this can be ignored.
        if (seqTracks) {
            List<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles = seqTrackService.returnExternallyProcessedMergedBamFiles(seqTracks)
            assert (!externallyProcessedMergedBamFiles || everythingVerified):
                    "There are ${externallyProcessedMergedBamFiles.size()} external merged bam files attached to this project. " +
                            "Clarify if the realignment shall be done anyway."
        }

        if (throwException) {
            println output
            throw new FileNotFoundException()
        }

        output << "delete content in db...\n\n"

        seqTracks.each { SeqTrack seqTrack ->
            File processingDir = new File(dataProcessingFilesService.getOutputDirectory(
                    seqTrack.individual, DataProcessingFilesService.OutputDirectories.MERGING))
            if (processingDir.exists()) {
                dirsToDelete.add(processingDir.path)
            }

            AbstractBamFile latestBamFile = MergingWorkPackage.findBySampleAndSeqType(seqTrack.sample, seqTrack.seqType)?.bamFileInProjectFolder
            if (latestBamFile) {
                File mergingDir = latestBamFile.baseDirectory
                if (mergingDir.exists()) {
                    List<ExternallyProcessedMergedBamFile> files = seqTrackService.returnExternallyProcessedMergedBamFiles([seqTrack])
                    files.each {
                        externalMergedBamFolders.add(it.nonOtpFolder.absoluteDataManagementPath.path)
                    }
                    mergingDir.listFiles().each {
                        dirsToDelete.add(it.path)
                    }
                }
            }
            deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, false).get("dirsToDelete").flatten().each {
                if (it) {
                    dirsToDelete.add(it.path)
                }
            }
        }

        Realm realm = configService.defaultRealm
        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "Delete_${projectName}.sh", realm)
        bashScriptToMoveFiles << dataSwapService.BASH_HEADER

        (dirsToDelete - externalMergedBamFolders).each {
            bashScriptToMoveFiles << "rm -rf ${it}\n"
        }

        output << "bash script to remove files on file system created:\n${bashScriptToMoveFiles}\n\n"

        println output

        return seqTracks
    }
}
