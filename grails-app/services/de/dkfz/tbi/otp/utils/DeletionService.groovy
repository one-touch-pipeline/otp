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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AnalysisDeletionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataswap.AbstractDataSwapService
import de.dkfz.tbi.otp.egaSubmission.EgaSubmission
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectInfo
import de.dkfz.tbi.otp.project.dta.DataTransferAgreement
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.utils.exceptions.FileNotFoundException
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector

import java.nio.file.Files
import java.nio.file.Path

import static org.springframework.util.Assert.notNull

/**
 * This class is written for scripts, so it needs the output in stdout.
 */
@CompileDynamic
@SuppressWarnings('Println')
@Transactional
class DeletionService {

    AbstractMergedBamFileService abstractMergedBamFileService
    AnalysisDeletionService analysisDeletionService
    CommentService commentService
    ConfigService configService
    DataProcessingFilesService dataProcessingFilesService
    FastqcDataFilesService fastqcDataFilesService
    FileService fileService
    IndividualService individualService
    LsdfFilesService lsdfFilesService
    RunService runService
    SeqTrackService seqTrackService
    WorkflowDeletionService workflowDeletionService

    void deleteProjectContent(Project project) {
        assert !project.archived

        assert !EgaSubmission.findAllByProject(project): "There are Ega Submissions connected to this Project, thus it can not be deleted"

        // Delete individuals for a project
        Individual.findAllByProject(project).each { individual ->
            deleteIndividual(individual, false)
        }

        // There are files which are not connected to a seqTrack -> they have to be deleted, too
        DataFile.findAllByProject(project).each { dataFile ->
            deleteDataFile(dataFile)
        }

        workflowDeletionService.deleteWorkflowVersionSelector(project)

        workflowDeletionService.deleteReferenceGenomeSelector(project)

        workflowDeletionService.deleteWorkflowRun(project)

        // remove project from ExternalWorkflowConfigSelector or delete selector completely
        deleteProjectsExternalWorkflowConfigSelector(project)
    }

    void deleteProject(Project project) {
        assert !project.archived

        deleteProjectContent(project)
        deleteProjectDependencies(project)
        project.delete(flush: true)
    }

    @SuppressWarnings('JavaIoPackageAccess')
    String deleteIndividual(Individual individual, boolean check = true) {
        assert !individual.project.archived

        StringBuilder deletionScript = new StringBuilder()

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
                List<File> seqTrackDirsToDelete = deleteSeqTrack(seqTrack, check)

                seqTrackDirsToDelete.each {
                    deletionScript << "rm -rf ${it.absolutePath}\n"
                }
                seqTypes.add(seqTrack.seqType)

                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(seqTrack.id.toString(), seqTrack.class.name))
            }

            SampleIdentifier.findAllBySample(sample)*.delete(flush: true)
            sample.delete(flush: true)
        }

        seqTypes.unique().each { SeqType seqType ->
            deletionScript << "rm -rf ${individualService.getViewByPidPath(individual, seqType)}\n"
        }

        deleteClusterJobs(ClusterJob.findAllByIndividual(individual))
        individual.delete(flush: true)

        return deletionScript.toString()
    }

    void deleteEmptyRun(Run run) {
        assert run: "The input run of the method deleteRun is null"
        deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(run.id.toString(), Run.name))
        run.delete(flush: true)
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
    @SuppressWarnings('JavaIoPackageAccess')
    List<SeqTrack> deleteProcessingFilesOfProject(String projectName, Path scriptOutputDirectory, boolean everythingVerified = false,
                                                  boolean ignoreWithdrawn = false, List<SeqTrack> explicitSeqTracks = []) throws FileNotFoundException {
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName(projectName))
        assert project: "Project does not exist"
        assert !project.archived

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
        assert !dataFiles.empty: "There are no SeqTracks attached to this project ${projectName}"

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
            throw new FileNotFoundException("Files not found")
        }

        output << "delete content in db...\n\n"

        seqTracks.each { SeqTrack seqTrack ->
            File processingDir = new File(dataProcessingFilesService.getOutputDirectory(
                    seqTrack.individual, DataProcessingFilesService.OutputDirectories.MERGING).toString())
            if (processingDir.exists()) {
                dirsToDelete.add(processingDir.path)
            }

            Set<AbstractBamFile> bamFiles = MergingWorkPackage.findAllBySampleAndSeqType(seqTrack.sample, seqTrack.seqType)*.bamFileInProjectFolder
            bamFiles.each { AbstractBamFile bamfile ->
                if (bamfile) {
                    Path mergingDir = abstractMergedBamFileService.getBaseDirectory(bamfile)
                    if (Files.exists(mergingDir)) {
                        List<ExternallyProcessedMergedBamFile> files = seqTrackService.returnExternallyProcessedMergedBamFiles([seqTrack])
                        files.each {
                            externalMergedBamFolders.add(it.nonOtpFolder.absoluteDataManagementPath.path)
                        }
                        Files.list(mergingDir).each {
                            dirsToDelete.add(it)
                        }
                    }
                }
            }
            deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, false).each {
                if (it) {
                    dirsToDelete.add(it.path)
                }
            }
        }

        Realm realm = configService.defaultRealm
        Path bashScriptToMoveFiles = fileService.createOrOverwriteScriptOutputFile(scriptOutputDirectory, "Delete_${projectName}.sh", realm)
        bashScriptToMoveFiles << AbstractDataSwapService.BASH_HEADER

        (dirsToDelete - externalMergedBamFolders).each {
            bashScriptToMoveFiles << "rm -rf ${it}\n"
        }

        output << "bash script to remove files on file system created:\n${bashScriptToMoveFiles}\n\n"

        println output

        return seqTracks
    }

    /**
     * Delete all processing information and results in the DB which are connected to one SeqTrack
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * !! Be aware that the run information and the seqTrack are not deleted.
     * !! If it is not needed to delete this information, this method can be used without pre-work.
     */
    List<File> deleteAllProcessingInformationAndResultOfOneSeqTrack(SeqTrack seqTrack, boolean enableChecks = true) {
        notNull(seqTrack, "The input seqTrack of the method deleteAllProcessingInformationAndResultOfOneSeqTrack is null")
        assert !seqTrack.project.archived
        List<File> dirsToDelete = []

        if (enableChecks) {
            seqTrackService.throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
            seqTrackService.throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
        }

        // for RoddyBamFiles
        MergingWorkPackage mergingWorkPackage = null
        List<RoddyBamFile> bamFilesFetch = RoddyBamFile.createCriteria().listDistinct {
            seqTracks {
                eq("id", seqTrack.id)
            }
        }
        List<RoddyBamFile> bamFiles = addRoddyBamFilesRecursively(bamFilesFetch).unique().sort {
            -it.id
        }

        if (bamFiles) {
            List<BamFilePairAnalysis> analyses = BamFilePairAnalysis.findAllBySampleType1BamFileInListOrSampleType2BamFileInList(bamFiles, bamFiles)
            List<SamplePair> samplePairs = SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(
                    bamFiles.first().workPackage,
                    bamFiles.first().workPackage)

            analyses.each {
                dirsToDelete << analysisDeletionService.deleteInstance(it)
                deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(it.id.toString(), it.class.name))
            }
            dirsToDelete.addAll(analysisDeletionService.deleteSamplePairsWithoutAnalysisInstances(samplePairs))
        }

        //Disconnect bam files from base bam files before starting with deleting of the list
        bamFiles.each { RoddyBamFile bamFile ->
            if (bamFile.baseBamFile) {
                bamFile.baseBamFile = null
                bamFile.save(flush: true, validate: false) //since object is deleted in next loop, no validation is necessary here
            }
        }

        //delete bam files
        bamFiles.each { RoddyBamFile bamFile ->
            mergingWorkPackage = bamFile.mergingWorkPackage
            mergingWorkPackage.bamFileInProjectFolder = null
            mergingWorkPackage.save(flush: true, validate: false) //since object is deleted later, no validation is necessary
            deleteQualityAssessmentInfoForAbstractBamFile(bamFile)
            deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(bamFile.id.toString(), bamFile.class.name))
            dirsToDelete << new File(abstractMergedBamFileService.getBaseDirectory(bamFile).toString())
            bamFile.delete(flush: true)
            // The MerginWorkPackage can only be deleted if all corresponding RoddyBamFiles are removed already
            if (!RoddyBamFile.findAllByWorkPackage(mergingWorkPackage)) {
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
            crmwp.save(flush: true, validate: false)
            deleteQualityAssessmentInfoForAbstractBamFile(bamFile)
            deleteProcessParameters(ProcessParameter.findAllByValueAndClassName(bamFile.id.toString(), bamFile.class.name))
            dirsToDelete << bamFile.workDirectory
            bamFile.delete(flush: true)
            if (!SingleCellBamFile.findAllByWorkPackage(crmwp)) {
                crmwp.delete(flush: true)
            }
        }

        List<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.createCriteria().list {
            seqTracks {
                eq('id', seqTrack.id)
            }
        }
        mergingWorkPackages.each {
            if (AbstractMergedBamFile.countByWorkPackage(it)) {
                it.seqTracks.remove(seqTrack)
                it.save(flush: true, validate: false)
            } else {
                SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(it, it)*.delete(flush: true)
                it.delete(flush: true)
            }
        }

        return dirsToDelete
    }

    /**
     * helper to include all bam files based on any given BamFiles recursively.
     */
    protected List<RoddyBamFile> addRoddyBamFilesRecursively(List<RoddyBamFile> roddyBamFiles) {
        return roddyBamFiles ? roddyBamFiles + addRoddyBamFilesRecursively(RoddyBamFile.findAllByBaseBamFileInList(roddyBamFiles)) : []
    }

    void deleteProcessParameters(List<ProcessParameter> processParameters) {
        Set<ProcessParameter> processParametersSet = collectProcessParametersRecursively([] as Set<ProcessParameter>, processParameters)
        Set<Process> processSet = processParametersSet*.process
        processParametersSet.each {
            it.delete(flush: true)
        }
        // delete all associations between processes
        // processes can only be safely deleted if no associations between them left
        processSet.each {
            it.restarted = null
            it.save(flush: true, validate: false)
        }.each {
            deleteProcess(it)
        }
    }

    private void deleteProjectDependencies(Project project) {
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

        DataTransferAgreement.findAllByProject(project)*.delete(flush: true)
    }

    private void deleteProcess(Process process) {
        assert process.finished: "process with id ${process.id} not finished"
        assert !ProcessParameter.findAllByProcess(process): "process with id ${process.id} has ProcessParameter attached to it. Delete association first."

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
            it.delete(flush: true)
        }
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
            error.delete(flush: true)
        }
        processingStepUpdate.delete(flush: true)
    }

    private Set<ProcessParameter> collectProcessParametersRecursively(
            Set<ProcessParameter> processParametersRecursionSet,
            List<ProcessParameter> processParameters
    ) {
        processParametersRecursionSet.addAll(processParameters)
        processParameters.each {
            Process.findAllByRestarted(it.process).each {
                collectProcessParametersRecursively(processParametersRecursionSet, ProcessParameter.findAllByProcess(it)).each {
                    processParametersRecursionSet << it
                }
            }
        }
        return processParametersRecursionSet
    }

    /**
     * Deletes one SeqTrack from the DB
     *
     * !! Be aware that the run information are not deleted.
     * There is always more than one seqTrack which belongs to one Run, which is why the run is not deleted.
     * !! If it is not needed to delete this information, this method can be used without pre-work.
     */
    List<File> deleteSeqTrack(SeqTrack seqTrack, boolean check = true) {
        notNull(seqTrack, "The input seqTrack of the method deleteSeqTrack is null")
        assert !seqTrack.project.archived

        if (check) {
            seqTrackService.throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
            seqTrackService.throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
        }

        //keep ilse reference for later deletion
        IlseSubmission ilseSubmission = seqTrack.ilseSubmission

        List<File> dirsToDelete = []
        List<File> seqTrackDelete = deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack, check)
        dirsToDelete.addAll(seqTrackDelete)

        List<DataFile> dataFiles = seqTrack.dataFiles

        DataFile.findAllBySeqTrack(seqTrack).each { DataFile df ->
            dirsToDelete.addAll(deleteDataFile(df))
        }

        Sample sample1 = seqTrack.sample
        SeqType seqType = seqTrack.seqType
        Individual individual = seqTrack.individual
        AntibodyTarget antibodyTarget = seqTrack.antibodyTarget

        seqTrack.delete(flush: true)

        List<SeqTrack> leftOverSeqTracks
        if (seqType.hasAntibodyTarget) {
            leftOverSeqTracks = SeqTrack.findAllBySampleAndAntibodyTarget(sample1, antibodyTarget)
        } else {
            leftOverSeqTracks = SeqTrack.findAllBySample(sample1)
        }
        List<ExternallyProcessedMergedBamFile> leftOverBamFiles = ExternallyProcessedMergedBamFile.withCriteria {
            'workPackage' {
                eq('sample', sample1)
                eq('seqType', seqType)
            }
        } as List<ExternallyProcessedMergedBamFile>

        if (!leftOverSeqTracks && !leftOverBamFiles) {
            dataFiles.collect {
                dirsToDelete.add(fileService.toFile(lsdfFilesService.getSampleTypeDirectory(it)))
            }
        } else {
            List<SeqTrack> seqTrackSampleList = SeqTrack.createCriteria().list {
                eq('sample', sample1)
                eq('seqType', seqType)
                if (seqType.hasAntibodyTarget) {
                    eq('antibodyTarget', antibodyTarget)
                }
            } as List<SeqTrack>
            if (seqTrackSampleList.empty) {
                dataFiles.collect {
                    dirsToDelete.add(fileService.toFile(lsdfFilesService.getSampleTypeDirectory(it)))
                }
            }
        }

        List<SeqTrack> seqTrackIndividualList = SeqTrack.createCriteria().list {
            sample {
                eq('individual', individual)
            }
            eq('seqType', seqType)
        } as List<SeqTrack>
        if (seqTrackIndividualList.empty) {
            dirsToDelete.add(fileService.toFile(individualService.getViewByPidPath(individual, seqType)))
        }

        //delete ilseSubmission if it is not used by other seqTracks and is not blacklisted
        if (ilseSubmission && !ilseSubmission.warning && SeqTrack.countByIlseSubmission(ilseSubmission) == 0) {
            ilseSubmission.delete(flush: true)
        }

        if (runService.isRunEmpty(seqTrack.run)) {
            deleteEmptyRun(seqTrack.run)
        }

        return dirsToDelete
    }

    /**
     * Removes all metadata-entries, which belong to the dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    private void deleteMetaDataEntryForDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFiles is null")
        MetaDataEntry.findAllByDataFile(dataFile)*.delete(flush: true)
    }

    /**
     * Removes all QA-Information & the MarkDuplicate-metrics for an AbstractBamFile.
     * As input the AbstractBamFile is chosen, so that the method can be used for ProcessedBamFiles and ProcessedMergedBamFiles.
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    private void deleteQualityAssessmentInfoForAbstractBamFile(AbstractBamFile abstractBamFile) {
        notNull(abstractBamFile, "The input AbstractBamFile is null")
        if (abstractBamFile instanceof RoddyBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(abstractBamFile)
            if (qualityAssessmentMergedPasses) {
                RoddyQualityAssessment.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete(flush: true)
            }
            qualityAssessmentMergedPasses*.delete(flush: true)
        } else if (abstractBamFile instanceof SingleCellBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByAbstractMergedBamFile(abstractBamFile)
            if (qualityAssessmentMergedPasses) {
                CellRangerQualityAssessment.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete(flush: true)
            }
            qualityAssessmentMergedPasses*.delete(flush: true)
        } else {
            throw new NotSupportedException("This BamFile type " + abstractBamFile + " is not supported")
        }
    }

    /**
     * Deletes a dataFile and all corresponding information
     */
    @SuppressWarnings('JavaIoPackageAccess')
    private List<File> deleteDataFile(DataFile dataFile) {
        notNull(dataFile, "The dataFile input of method deleteDataFile is null")

        String fileFinalPath = lsdfFilesService.getFileFinalPath(dataFile)
        List<File> dirs = [
                fileFinalPath,
                "${fileFinalPath}.md5sum",
                lsdfFilesService.getFileViewByPidPath(dataFile),
        ].collect { new File(it) }

        dirs.addAll(deleteFastQCInformationFromDataFile(dataFile))
        deleteMetaDataEntryForDataFile(dataFile)
        dataFile.delete(flush: true)
        return dirs
    }

    /**
     * Removes all fastQC information about the dataFile
     */
    @SuppressWarnings('JavaIoPackageAccess')
    private List<File> deleteFastQCInformationFromDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFile is null")
        List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllByDataFile(dataFile)
        if (fastqcProcessedFiles) {
            String fastqFile = fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFiles.first())
            List<File> files = [
                    fastqFile,
                    "${fastqFile}.md5sum",
            ].collect {
                new File(it)
            }
            fastqcProcessedFiles*.delete(flush: true)
            return files
        }
        return []
    }

    /**
     * Either remove project from all dependent external workflow config selectors or delete selectors completely
     * if selectors are only dependent on this one project.
     *
     * @param project which should be remove from the selectors.
     */
    void deleteProjectsExternalWorkflowConfigSelector(Project project) {
        if (project) {
            ExternalWorkflowConfigSelector.withCriteria {
                projects {
                    eq('id', project.id)
                }
            }.each {
                if (it.projects.size() > 1) {
                    it.projects.remove(project)
                } else {
                    it.delete(flush: true)
                }
            }
        }
    }
}
