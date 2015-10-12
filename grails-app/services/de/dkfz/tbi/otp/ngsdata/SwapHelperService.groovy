package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvDeletionService
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus
import de.dkfz.tbi.otp.ngsqc.*

import static org.springframework.util.Assert.notNull


/**
 * This script contains all methods, which are shared between the different swapping-scripts
 */
class SwapHelperService {
    SeqTrackService seqTrackService
    SnvDeletionService snvDeletionService


    /**
     * Removes all fastQC information about the dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteFastQCInformationFromDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFile is null")
        List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllByDataFile(dataFile)

        FastqcBasicStatistics.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()
        FastqcModuleStatus.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()
        FastqcOverrepresentedSequences.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()
        FastqcPerBaseSequenceAnalysis.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()
        FastqcPerSequenceGCContent.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()
        FastqcPerSequenceQualityScores.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()
        FastqcKmerContent.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()
        FastqcSequenceDuplicationLevels.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()
        FastqcSequenceLengthDistribution.findAllByFastqcProcessedFileInList(fastqcProcessedFiles)*.delete()

        fastqcProcessedFiles*.delete()
    }


    /**
     * Removes all metadata-entries, which belong to the dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteMetaDataEntryForDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFiles is null")
        MetaDataEntry.findAllByDataFile(dataFile)*.delete()
    }


    /**
     * Removes all information about the consistency checks of the input dataFile
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteConsistencyStatusInformationForDataFile(DataFile dataFile) {
        notNull(dataFile, "The input dataFiles is null")
        ConsistencyStatus.findAllByDataFile(dataFile)*.delete()
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

            ChromosomeQualityAssessment.findAllByQualityAssessmentPassInList(qualityAssessmentPasses)*.delete()
            OverallQualityAssessment.findAllByQualityAssessmentPassInList(qualityAssessmentPasses)*.delete()
            qualityAssessmentPasses*.delete()
        } else if (abstractBamFile instanceof ProcessedMergedBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByProcessedMergedBamFile(abstractBamFile)

            ChromosomeQualityAssessmentMerged.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete()
            OverallQualityAssessmentMerged.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete()
            qualityAssessmentMergedPasses*.delete()
        } else if (abstractBamFile instanceof RoddyBamFile) {
            List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses = QualityAssessmentMergedPass.findAllByProcessedMergedBamFile(abstractBamFile)
            RoddyQualityAssessment.findAllByQualityAssessmentMergedPassInList(qualityAssessmentMergedPasses)*.delete()
            qualityAssessmentMergedPasses*.delete()
        } else {
            throw new RuntimeException("This BamFile type " + abstractBamFile + " is not supported")
        }

        PicardMarkDuplicatesMetrics.findAllByAbstractBamFile(abstractBamFile)*.delete()
    }


    /**
     * Deletes all studies and corresponding study samples which belong to this project
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteStudiesOfOneProject(Project project) {
        notNull(project, "The input project is null")

        List<Study> studies = Study.findAllByProject(project)
        if(!studies.empty) {
            StudySample.findAllByStudyInList(studies)*.delete()
        }
        studies*.delete()
    }


    /**
     * Deletes all mutations and corresponding result data files
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    void deleteMutationsAndResultDataFilesOfOneIndividual(Individual individual) {
        notNull(individual, "The input individual is null")
        List<Mutation> mutations = Mutation.findAllByIndividual(individual)
        List<ResultsDataFile> resultDataFiles = mutations*.resultsDataFile
        mutations*.delete()
        resultDataFiles*.delete()
    }


    /**
     * Delete merging related database entries, based on the mergingSetAssignments
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     */
    List<File> deleteMergingRelatedConnectionsOfBamFile(ProcessedBamFile processedBamFile) {
        notNull(processedBamFile, "The input processedBamFile is null in method deleteMergingRelatedConnectionsOfBamFile")
        List<File> dirsToDelete = []

        List<MergingSetAssignment> mergingSetAssignments = MergingSetAssignment.findAllByBamFile(processedBamFile)
        List<MergingSet> mergingSets = mergingSetAssignments*.mergingSet
        List<MergingWorkPackage> mergingWorkPackages = mergingSets*.mergingWorkPackage

        if (mergingWorkPackages.empty) {
            println "there is no merging for processedBamFile " + processedBamFile
        } else if (mergingWorkPackages.unique().size() > 1) {
            throw new RuntimeException("There is not one unique mergingWorkPackage for ProcessedBamFile " + processedBamFile)
        } else {
            MergingWorkPackage mergingWorkPackage = mergingWorkPackages.first()
            List<MergingPass> mergingPasses = MergingPass.findAllByMergingSetInList(mergingSets).unique()
            List<ProcessedMergedBamFile> processedMergedBamFiles = ProcessedMergedBamFile.findAllByMergingPassInList(mergingPasses)

            mergingSetAssignments*.delete()

            processedMergedBamFiles.each { ProcessedMergedBamFile processedMergedBamFile ->
                deleteQualityAssessmentInfoForAbstractBamFile(processedMergedBamFile)
                MergingSetAssignment.findAllByBamFile(processedMergedBamFile)*.delete()
                dirsToDelete << snvDeletionService.deleteForAbstractMergedBamFile(processedMergedBamFile)
                processedMergedBamFile.delete()
            }

            mergingPasses*.delete()

            mergingSets.each { MergingSet mergingSet ->
                // The MergingSet can only be deleted if all corresponding AbstractBamFiles are removed already
                if (!MergingSetAssignment.findByMergingSet(mergingSet)) {
                    mergingSet.delete()
                }
            }
            // The MerginWorkPackage can only be deleted if all corresponding MergingSets and AlignmentPasses are removed already
            if (!MergingSet.findByMergingWorkPackage(mergingWorkPackage) && !AlignmentPass.findByWorkPackage(mergingWorkPackage)) {
                mergingWorkPackage.delete()
            }
        }
        return dirsToDelete
    }


    /**
     * Deletes a dataFile and all corresponding information
     */
    void deleteDataFile(DataFile dataFile) {
        notNull(dataFile, "The dataFile input of method deleteDataFile is null")

        deleteFastQCInformationFromDataFile(dataFile)
        deleteMetaDataEntryForDataFile(dataFile)
        deleteConsistencyStatusInformationForDataFile(dataFile)
        dataFile.delete()
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
            alignmentLog.delete()
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
    List<File> deleteAllProcessingInformationAndResultOfOneSeqTrack(SeqTrack seqTrack) {
        notNull(seqTrack, "The input seqTrack of the method deleteAllProcessingInformationAndResultOfOneSeqTrack is null")
        List<File> dirsToDelete = []

        throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])

        List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)

        List<ProcessedSaiFile> processedSaiFiles = ProcessedSaiFile.findAllByDataFileInList(dataFiles)*.delete()

        // for ProcessedMergedBamFiles
        AlignmentPass.findAllBySeqTrack(seqTrack).each { AlignmentPass alignmentPass ->
            MergingWorkPackage mergingWorkPackage = alignmentPass.workPackage
            mergingWorkPackage.bamFileInProjectFolder = null
            mergingWorkPackage.save(flush: true)
            ProcessedBamFile.findAllByAlignmentPass(alignmentPass).each { ProcessedBamFile processedBamFile ->

                deleteQualityAssessmentInfoForAbstractBamFile(processedBamFile)
                dirsToDelete << deleteMergingRelatedConnectionsOfBamFile(processedBamFile)

                processedBamFile.delete()
            }
            alignmentPass.delete()
            // The MerginWorkPackage can only be deleted if all corresponding MergingSets and AlignmentPasses are removed already
            if (!MergingSet.findByMergingWorkPackage(mergingWorkPackage) && !AlignmentPass.findByWorkPackage(mergingWorkPackage)) {
                snvDeletionService.deleteSamplePairsWithoutSnvCallingInstances(SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
                mergingWorkPackage.delete(flush: true)
            }
        }

        // for RoddyBamFiles
        MergingWorkPackage mergingWorkPackage = null
        List<RoddyBamFile> bamFiles = RoddyBamFile.createCriteria().list {
            seqTracks {
                eq("id", seqTrack.id)
            }
        }
        bamFiles.each { RoddyBamFile bamFile ->
            mergingWorkPackage = bamFile.mergingWorkPackage
            mergingWorkPackage.bamFileInProjectFolder = null
            mergingWorkPackage.save(flush: true)
            deleteQualityAssessmentInfoForAbstractBamFile(bamFile)
            dirsToDelete << snvDeletionService.deleteForAbstractMergedBamFile(bamFile)
            bamFile.delete()
            // The MerginWorkPackage can only be deleted if all corresponding RoddyBamFiles are removed already
            if (!RoddyBamFile.findByWorkPackage(mergingWorkPackage)) {
                snvDeletionService.deleteSamplePairsWithoutSnvCallingInstances(SamplePair.findAllByMergingWorkPackage1OrMergingWorkPackage2(mergingWorkPackage, mergingWorkPackage))
                mergingWorkPackage.delete(flush: true)
            }
        }
        return dirsToDelete
    }


    /**
     * Deletes the SeqScan, corresponding information and its connections to the fastqfiles/seqTracks
     */
    void deleteSeqScanAndCorrespondingInformation(SeqScan seqScan) {
        notNull(seqScan, "The input seqScan of the method deleteSeqScanAndCorrespondingInformation is null")

        List<MergingLog> mergingLogs = MergingLog.findAllBySeqScan(seqScan)
        MergedAlignmentDataFile.findAllByMergingLogInList(mergingLogs)*.delete()
        mergingLogs*.delete()
        seqScan.delete()
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
    List<File> deleteSeqTrack(SeqTrack seqTrack) {
        notNull(seqTrack, "The input seqTrack of the method deleteSeqTrack is null")
        List<File> dirsToDelete = []

        throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])

        dirsToDelete << deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack)
        deleteConnectionFromSeqTrackRepresentingABamFile(seqTrack)
        DataFile.findAllBySeqTrack(seqTrack).each { deleteDataFile(it) }
        MergingAssignment.findAllBySeqTrack(seqTrack)*.delete()

        seqTrack.delete()
        return dirsToDelete
    }


    /**
     * Deletes the given runs.
     * For each run it has to be checked if it can be deleted.
     * Only when all RunSegments from this Run can be deleted, also the Run can be deleted.
     * A RunSegment can be deleted when all dataFiles, which belong to this RunSegment, are from the project, which has to be deleted.
     * If not the RunSegment, and therefore also the run, has to stay in the DB.
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * !! Be aware that the dataFiles have to be deleted already, before calling this method ->Therefore the runs have to be collected first.
     * !! Furthermore the seqTracks have to be deleted first.
     */
    void deleteRunAndRunSegmentsWithoutDataOfOtherProjects(Run run, Project project) {
        notNull(run, "The input run of the method deleteRunAndRunSegmentsWithoutDataOfOtherProjects is null")
        notNull(project, "The input project of the method deleteRunAndRunSegmentsWithoutDataOfOtherProjects is null")

        RunByProject.findByRunAndProject(run, project)*.delete()

        boolean allRunSegmentsOfTheRunCanBeDeleted = true
        List<RunSegment> runSegmentsPerRun = RunSegment.findAllByRun(run)

        runSegmentsPerRun.each { RunSegment runSegment ->
            List<MetaDataFile> metaDataFiles = MetaDataFile.findAllByRunSegment(runSegment)
            List<DataFile> dataFilesPerRunSegment = DataFile.findAllByRunSegment(runSegment)

            if (dataFilesPerRunSegment.empty) {
                metaDataFiles*.delete()
                runSegment.delete()
            } else if (dataFilesPerRunSegment.find { it.project == project }) {
                throw new RuntimeException("Although all dataFiles of this run should be deleted already it is not the case.")
            } else {
                allRunSegmentsOfTheRunCanBeDeleted = false
                println "RunSegment " + runSegment.dataPath + " can not be deleted, since it contains data from other projects."
            }
        }

        if (allRunSegmentsOfTheRunCanBeDeleted) {
            run.delete()
        } else {
            println "The run " + run + " can not be deleted completely, since it also contains RunSegments from other projects."
        }
    }


    /**
     * Deletes the given run with all connected data.
     * This includes:
     * - all referenced datafiles using deleteDataFile
     * - all seqtrack using deleteSeqTrack
     * - all metadata files
     * - all run segments
     * - the run itself
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     */
    List<File> deleteRun(Run run, StringBuilder outputStringBuilder) {
        notNull(run, "The input run of the method deleteRun is null")
        outputStringBuilder << "\n\nstart deletion of run ${run}"
        List<File> dirsToDelete = []

        RunByProject.findByRun(run)*.delete()

        List<RunSegment> runSegmentsPerRun = RunSegment.findAllByRun(run)

        outputStringBuilder << "\n  delete data files: "
        DataFile.findAllByRun(run).each {
            outputStringBuilder << "\n     try to delete: ${it}"
            deleteDataFile(it)
        }

        outputStringBuilder << "\n  delete seqTracks:"
        SeqTrack.findAllByRun(run).each {
            outputStringBuilder << "\n     try to delete: ${it}"
            dirsToDelete = deleteSeqTrack(it)
        }


        outputStringBuilder << "\n  delete meta data files:"
        MetaDataFile.findAllByRunSegmentInList(runSegmentsPerRun).each {
            outputStringBuilder << "\n     try to delete: ${it}"
            it.delete()
        }


        outputStringBuilder << "\n  delete run segments:"
        runSegmentsPerRun.each { RunSegment runSegment ->
            outputStringBuilder << "\n     try to delete: ${runSegment}"
            runSegment.delete()
        }

        outputStringBuilder << "\n  try to delete run ${run}"
        run.delete()
        outputStringBuilder << "\nfinish deletion of run ${run}"
        return dirsToDelete
    }


    /**
     * Deletes the run given by Name.
     * The method fetch the run for the name and call then deleteRun.
     *
     * The function should be called inside a transaction (DOMAIN.withTransaction{}) to roll back changes if an exception occurs or a check fails.
     *
     * @see #deleteRun(de.dkfz.tbi.otp.ngsdata.Run, java.lang.StringBuilder)
     */
    List<File> deleteRunByName(String runName, StringBuilder outputStringBuilder) {
        notNull(runName, "The input runName of the method deleteRunByName is null")
        Run run = Run.findByName(runName)
        notNull(run, "No run with name ${runName} could be found in the database")
        List<File> dirsToDelete = deleteRun(run, outputStringBuilder)
        outputStringBuilder << "the following directories needs to be deleted manually: "
        outputStringBuilder << dirsToDelete.flatten()*.path.join('\n')
        outputStringBuilder << "And do not forget the other files/directories which belongs to the run"
        return dirsToDelete
    }

    void throwExceptionInCaseOfExternalMergedBamFileIsAttached(List<SeqTrack> seqTracks) {
        // In case there are ExternallyProcessedMergedBamFile attached to the lanes to swap, the script shall stop
        List<ExternallyProcessedMergedBamFile> externallyProcessedMergedBamFiles = seqTrackService.returnExternallyProcessedMergedBamFiles(seqTracks)
        assert externallyProcessedMergedBamFiles.empty : "There are ExternallyProcessedMergedBamFiles attached: ${externallyProcessedMergedBamFiles}"
    }


    /**
     * A transaction wrapper for a callback.
     * The closure execute the given closure inside a transaction and assert at the end the value of rollback.
     * The transaction ensures, that if an exception occur in the script, all database changes are roll backed.
     * The rollback flag allows to trigger a rollback at the of the transaction to ensure, that nothing is changed.
     * This allows to test the script without changing the database.
     */
    void transaction(boolean rollback, Closure c, StringBuilder outputStringBuilder) {
        try {
            Project.withTransaction {
                c()
                assert !rollback
            }
        } catch (Throwable t) {
            outputStringBuilder << "\n\n${t}"
            t.getStackTrace().each { outputStringBuilder << "\n    ${it}" }
            println outputStringBuilder
        }
    }
}
