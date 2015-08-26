package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus
import de.dkfz.tbi.otp.ngsqc.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase
import org.junit.Test


class SwapHelperServiceTests extends GroovyScriptAwareTestCase {
    SwapHelperService swapHelperService


    @Test
    public void testDeleteFastQCInformationFromDataFile() throws Exception {
        DataFile dataFile = DataFile.build()
        FastqcProcessedFile fastqcProcessedFile = FastqcProcessedFile.build(dataFile: dataFile)
        FastqcBasicStatistics fastqcBasicStatistics = FastqcBasicStatistics.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcModuleStatus fastqcModuleStatus = FastqcModuleStatus.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcOverrepresentedSequences fastqcOverrepresentedSequences = FastqcOverrepresentedSequences.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerBaseSequenceAnalysis fastqcPerBaseSequenceAnalysis = FastqcPerBaseSequenceAnalysis.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerSequenceGCContent fastqcPerSequenceGCContent = FastqcPerSequenceGCContent.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerSequenceQualityScores fastqcPerSequenceQualityScores = FastqcPerSequenceQualityScores.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcKmerContent fastqcKmerContent = FastqcKmerContent.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcSequenceDuplicationLevels fastqcSequenceDuplicationLevels = FastqcSequenceDuplicationLevels.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.build(fastqcProcessedFile: fastqcProcessedFile)

        swapHelperService.deleteFastQCInformationFromDataFile(dataFile)

        assert !FastqcProcessedFile.get(fastqcProcessedFile.id)
        assert !FastqcBasicStatistics.get(fastqcBasicStatistics.id)
        assert !FastqcModuleStatus.get(fastqcModuleStatus.id)
        assert !FastqcOverrepresentedSequences.get(fastqcOverrepresentedSequences.id)
        assert !FastqcPerBaseSequenceAnalysis.get(fastqcPerBaseSequenceAnalysis.id)
        assert !FastqcPerSequenceGCContent.get(fastqcPerSequenceGCContent.id)
        assert !FastqcPerSequenceQualityScores.get(fastqcPerSequenceQualityScores.id)
        assert !FastqcKmerContent.get(fastqcKmerContent.id)
        assert !FastqcSequenceDuplicationLevels.get(fastqcSequenceDuplicationLevels.id)
        assert !FastqcSequenceLengthDistribution.get(fastqcSequenceLengthDistribution.id)
    }

    @Test
    public void testDeleteMetaDataEntryForDataFile() throws Exception {
        DataFile dataFile = DataFile.build()
        MetaDataEntry metaDataEntry = MetaDataEntry.build(dataFile: dataFile)

        swapHelperService.deleteMetaDataEntryForDataFile(dataFile)

        assert !MetaDataEntry.get(metaDataEntry.id)
    }

    @Test
    public void testDeleteConsistencyStatusInformationForDataFile() throws Exception {
        DataFile dataFile = DataFile.build()
        ConsistencyStatus consistencyStatus = ConsistencyStatus.build(dataFile: dataFile)

        swapHelperService.deleteConsistencyStatusInformationForDataFile(dataFile)

        assert !ConsistencyStatus.get(consistencyStatus.id)
    }


    @Test
    public void testDeleteQualityAssessmentInfoForAbstractBamFile_ProcessedBamFile() throws Exception {
        AbstractBamFile abstractBamFile = ProcessedBamFile.build()

        QualityAssessmentPass qualityAssessmentPass = QualityAssessmentPass.build(processedBamFile: abstractBamFile)
        ChromosomeQualityAssessment chromosomeQualityAssessment = ChromosomeQualityAssessment.build(qualityAssessmentPass: qualityAssessmentPass)
        OverallQualityAssessment overallQualityAssessment = OverallQualityAssessment.build(qualityAssessmentPass: qualityAssessmentPass)

        swapHelperService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessment.get(chromosomeQualityAssessment.id)
        assert !OverallQualityAssessment.get(overallQualityAssessment.id)
    }

    @Test
    public void testDeleteQualityAssessmentInfoForAbstractBamFile_ProcessedMergedBamFile() throws Exception {
        AbstractBamFile abstractBamFile = DomainFactory.createProcessedMergedBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = QualityAssessmentMergedPass.build(abstractMergedBamFile: abstractBamFile)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessment = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentPass)
        OverallQualityAssessmentMerged overallQualityAssessment = OverallQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentPass)
        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = PicardMarkDuplicatesMetrics.build(abstractBamFile: abstractBamFile)

        swapHelperService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessmentMerged.get(chromosomeQualityAssessment.id)
        assert !OverallQualityAssessmentMerged.get(overallQualityAssessment.id)
        assert !PicardMarkDuplicatesMetrics.get(picardMarkDuplicatesMetrics.id)
    }

    @Test
    public void testDeleteQualityAssessmentInfoForAbstractBamFile_RoddyBamFile() throws Exception {
        AbstractBamFile abstractBamFile = DomainFactory.createRoddyBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = QualityAssessmentMergedPass.build(abstractMergedBamFile: abstractBamFile)
        RoddyQualityAssessment roddyQualityAssessment = RoddyQualityAssessment.build(qualityAssessmentMergedPass: qualityAssessmentPass)
        RoddyMergedBamQa roddyMergedBamQa = RoddyMergedBamQa.build(qualityAssessmentMergedPass: qualityAssessmentPass)
        RoddySingleLaneQa roddySingleLaneQa = RoddySingleLaneQa.build(seqTrack: abstractBamFile.seqTracks.iterator().next(), qualityAssessmentMergedPass: qualityAssessmentPass)

        swapHelperService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessmentMerged.get(roddyQualityAssessment.id)
        assert !RoddyMergedBamQa.get(roddyMergedBamQa.id)
        assert !RoddySingleLaneQa.get(roddySingleLaneQa.id)
    }

    @Test
    public void testDeleteQualityAssessmentInfoForAbstractBamFile_null() throws Exception {
        AbstractBamFile abstractBamFile = null

        final shouldFail = new GroovyTestCase().&shouldFail
        String message = shouldFail RuntimeException, {
            swapHelperService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)
        }
        assert message == "The input AbstractBamFile is null"
    }

    @Test
    public void testDeleteStudiesOfOneProject() throws Exception {
        Project project = Project.build()
        Study study = Study.build(project: project)
        StudySample studySample = StudySample.build(study: study)

        swapHelperService.deleteStudiesOfOneProject(project)

        assert !Study.get(study.id)
        assert !StudySample.get(studySample.id)
    }

    @Test
    public void testDeleteMutationsAndResultDataFilesOfOneIndividual() throws Exception {
        Individual individual = Individual.build()
        ResultsDataFile resultsDataFile = ResultsDataFile.build()
        Mutation mutation = Mutation.build(individual: individual, resultsDataFile: resultsDataFile)

        swapHelperService.deleteMutationsAndResultDataFilesOfOneIndividual(individual)

        assert !ResultsDataFile.get(resultsDataFile.id)
        assert !Mutation.get(mutation.id)
    }


    @Test
    public void testDeleteMergingRelatedConnectionsOfBamFile() throws Exception {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build(workflow: Workflow.build(name: Workflow.Name.DEFAULT_OTP))
        MergingSet mergingSet = MergingSet.build(mergingWorkPackage: mergingWorkPackage)
        ProcessedBamFile processedBamFile = DomainFactory.createProcessedBamFile(mergingWorkPackage)
        MergingPass mergingPass = MergingPass.build(mergingSet: mergingSet)
        MergingSetAssignment mergingSetAssignment = MergingSetAssignment.build(bamFile: processedBamFile, mergingSet: mergingSet)
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.build(workPackage: mergingWorkPackage, mergingPass: mergingPass)

        swapHelperService.deleteMergingRelatedConnectionsOfBamFile(processedBamFile)

        assert !MergingPass.get(mergingPass.id)
        assert !MergingSet.get(mergingSet.id)
        assert !MergingSetAssignment.get(mergingSetAssignment.id)
        assert !ProcessedMergedBamFile.get(bamFile.id)
    }

    @Test
    public void testDeleteDataFile() throws Exception {
        DataFile dataFile = DataFile.build()
        FastqcProcessedFile fastqcProcessedFile = FastqcProcessedFile.build(dataFile: dataFile)
        FastqcBasicStatistics fastqcBasicStatistics = FastqcBasicStatistics.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcModuleStatus fastqcModuleStatus = FastqcModuleStatus.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcOverrepresentedSequences fastqcOverrepresentedSequences = FastqcOverrepresentedSequences.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerBaseSequenceAnalysis fastqcPerBaseSequenceAnalysis = FastqcPerBaseSequenceAnalysis.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerSequenceGCContent fastqcPerSequenceGCContent = FastqcPerSequenceGCContent.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerSequenceQualityScores fastqcPerSequenceQualityScores = FastqcPerSequenceQualityScores.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcKmerContent fastqcKmerContent = FastqcKmerContent.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcSequenceDuplicationLevels fastqcSequenceDuplicationLevels = FastqcSequenceDuplicationLevels.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.build(fastqcProcessedFile: fastqcProcessedFile)

        MetaDataEntry metaDataEntry = MetaDataEntry.build(dataFile: dataFile)

        ConsistencyStatus consistencyStatus = ConsistencyStatus.build(dataFile: dataFile)

        swapHelperService.deleteDataFile(dataFile)

        assert !FastqcProcessedFile.get(fastqcProcessedFile.id)
        assert !FastqcBasicStatistics.get(fastqcBasicStatistics.id)
        assert !FastqcModuleStatus.get(fastqcModuleStatus.id)
        assert !FastqcOverrepresentedSequences.get(fastqcOverrepresentedSequences.id)
        assert !FastqcPerBaseSequenceAnalysis.get(fastqcPerBaseSequenceAnalysis.id)
        assert !FastqcPerSequenceGCContent.get(fastqcPerSequenceGCContent.id)
        assert !FastqcPerSequenceQualityScores.get(fastqcPerSequenceQualityScores.id)
        assert !FastqcKmerContent.get(fastqcKmerContent.id)
        assert !FastqcSequenceDuplicationLevels.get(fastqcSequenceDuplicationLevels.id)
        assert !FastqcSequenceLengthDistribution.get(fastqcSequenceLengthDistribution.id)
        assert !MetaDataEntry.get(metaDataEntry.id)
        assert !ConsistencyStatus.get(consistencyStatus.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    public void testDeleteConnectionFromSeqTrackRepresentingABamFile() throws Exception {
        SeqTrack seqTrack = SeqTrack.build()
        AlignmentLog alignmentLog = AlignmentLog.build(seqTrack: seqTrack)
        DataFile dataFile = DataFile.build(alignmentLog: alignmentLog)

        swapHelperService.deleteConnectionFromSeqTrackRepresentingABamFile(seqTrack)

        assert !AlignmentLog.get(alignmentLog.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    public void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_ProcessedBamFile() throws Exception {
        SeqTrack seqTrack = SeqTrack.build()
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build(dataFile: dataFile)

        TestData testData = new TestData()
        AlignmentPass alignmentPass = testData.createAlignmentPass(seqTrack: seqTrack)
        MergingWorkPackage workPackage = alignmentPass.workPackage
        MergingSet mergingSet = MergingSet.build(mergingWorkPackage: workPackage)
        MergingPass mergingPass = MergingPass.build(mergingSet: mergingSet)

        AbstractMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build(mergingPass: mergingPass, fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS, workPackage: workPackage)
        workPackage.bamFileInProjectFolder = processedMergedBamFile
        workPackage.save(flush: true, failOnError: true)
        alignmentPass.save(flush: true, failOnError: true)

        swapHelperService.deleteAllProcessingInformationAndResultOfOneSeqTrack(alignmentPass.seqTrack)

        assert !ProcessedSaiFile.get(processedSaiFile.id)
        assert !ProcessedBamFile.get(processedMergedBamFile.id)
        assert !AlignmentPass.get(alignmentPass.id)
    }


    @Test
    public void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_RoddyBamFile() throws Exception {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.workPackage.bamFileInProjectFolder = roddyBamFile
        roddyBamFile.workPackage.save(flush: true)

        swapHelperService.deleteAllProcessingInformationAndResultOfOneSeqTrack(roddyBamFile.seqTracks.iterator().next())

        assert !RoddyBamFile.get(roddyBamFile.id)
        assert !MergingWorkPackage.get(roddyBamFile.workPackage.id)
    }


    @Test
    public void testDeleteSeqScanAndCorrespondingInformation() throws Exception {
        SeqScan seqScan = SeqScan.build()
        MergingLog mergingLog = MergingLog.build(seqScan: seqScan)
        MergedAlignmentDataFile mergedAlignmentDataFile = MergedAlignmentDataFile.build(mergingLog: mergingLog)

        swapHelperService.deleteSeqScanAndCorrespondingInformation(seqScan)

        assert !MergingLog.get(mergingLog.id)
        assert !MergedAlignmentDataFile.get(mergedAlignmentDataFile.id)
        assert !SeqScan.get(seqScan.id)
    }

    @Test
    public void testDeleteSeqTrack() throws Exception {
        SeqTrack seqTrack = SeqTrack.build()
        MergingAssignment mergingAssignment = MergingAssignment.build(seqTrack: seqTrack)
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)

        swapHelperService.deleteSeqTrack(seqTrack)

        assert !SeqTrack.get(seqTrack.id)
        assert !MergingAssignment.get(mergingAssignment.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    public void testDeleteRunAndRunSegmentsWithoutDataOfOtherProjects() throws Exception {
        Run run = Run.build()
        Project project = Project.build()
        RunByProject runByProject = RunByProject.build(run: run, project: project)
        RunSegment runSegment = RunSegment.build(run: run)

        swapHelperService.deleteRunAndRunSegmentsWithoutDataOfOtherProjects(run, project)

        assert !RunByProject.get(runByProject.id)
        assert !Run.get(run.id)
        assert !RunSegment.get(runSegment.id)
    }

    @Test
    public void testDeleteRun() throws Exception {
        StringBuilder outputStringBuilder = new StringBuilder()
        Run run = Run.build()
        RunByProject runByProject = RunByProject.build(run: run)
        RunSegment runSegment = RunSegment.build(run: run)
        DataFile dataFile = DataFile.build(run: run)
        MetaDataFile metaDataFile = MetaDataFile.build(runSegment: runSegment)

        swapHelperService.deleteRun(run, outputStringBuilder)

        assert !Run.get(run.id)
        assert !RunByProject.get(runByProject.id)
        assert !RunSegment.get(runSegment.id)
        assert !DataFile.get(dataFile.id)
        assert !MetaDataFile.get(metaDataFile.id)
    }

    @Test
    public void testDeleteRunByName() throws Exception {
        StringBuilder outputStringBuilder = new StringBuilder()
        Run run = Run.build()
        RunByProject runByProject = RunByProject.build(run: run)
        RunSegment runSegment = RunSegment.build(run: run)
        DataFile dataFile = DataFile.build(run: run)
        MetaDataFile metaDataFile = MetaDataFile.build(runSegment: runSegment)

        swapHelperService.deleteRunByName(run.name, outputStringBuilder)

        assert !Run.get(run.id)
        assert !RunByProject.get(runByProject.id)
        assert !RunSegment.get(runSegment.id)
        assert !DataFile.get(dataFile.id)
        assert !MetaDataFile.get(metaDataFile.id)
    }

    @Test
    public void testThrowExceptionInCaseOfExternalMergedBamFileIsAttached() throws Exception {
        SeqTrack seqTrack = SeqTrack.build()
        FastqSet fastqSet = FastqSet.build(seqTracks: [seqTrack])
        ExternallyProcessedMergedBamFile.build(fastqSet: fastqSet, type: AbstractBamFile.BamType.RMDUP)

        final shouldFail = new GroovyTestCase().&shouldFail
        shouldFail AssertionError, {
            swapHelperService.throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
        }
    }
}
