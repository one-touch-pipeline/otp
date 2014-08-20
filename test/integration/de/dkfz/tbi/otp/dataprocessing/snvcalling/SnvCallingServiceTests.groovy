package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.dataprocessing.snvcalling.ConfigPerProjectAndSeqType.Purpose
import static de.dkfz.tbi.otp.utils.CollectionUtils.*


class SnvCallingServiceTests {

    TestData testData
    SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual

    SnvCallingService snvCallingService
    ConfigPerProjectAndSeqType configPerProjectAndSeqType
    ProcessedMergedBamFile processedMergedBamFile1
    ProcessedMergedBamFile processedMergedBamFile2
    Project project
    Individual individual
    SeqType seqType

    final static double COVERAGE_THRESHOLD = 30.0
    final static double COVERAGE_TOO_LOW = 20.0

    final static int LANE_THRESHOLD = 3
    final static int TOO_LITTLE_LANES = 2

    @Before
    void setUp() {
        testData = new TestData()
        testData.createObjects()

        project = testData.createProject()
        project.save()

        individual = testData.createIndividual([project: project, pid: "testPid"])
        individual.save()

        seqType = testData.createSeqType()
        seqType.save()

        configPerProjectAndSeqType = new ConfigPerProjectAndSeqType(
                project: project,
                seqType: seqType,
                purpose: Purpose.SNV,
                configuration: "configuration"
                )
        configPerProjectAndSeqType.save()

        processedMergedBamFile1 = createProcessedMergedBamFile("1")
        processedMergedBamFile1.save()

        processedMergedBamFile2 = createProcessedMergedBamFile("2")
        processedMergedBamFile2.save()

        sampleTypeCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: processedMergedBamFile1.sample.sampleType,
                sampleType2: processedMergedBamFile2.sample.sampleType,
                seqType: seqType
                )
        sampleTypeCombinationPerIndividual.save()
    }

    @After
    void tearDown() {
        sampleTypeCombinationPerIndividual = null
        configPerProjectAndSeqType = null
        processedMergedBamFile1 = null
        processedMergedBamFile2 = null
        project = null
        individual = null
        seqType = null
    }

    @Test
    void testSamplePairForSnvProcessingAllCorrect() {
        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testConfigPerProjectAndSeqTypeIsNullOtherProject() {
        Project otherProject = new Project(name: "otherProject", dirName: "/tmp", realmName: "DKFZ")
        otherProject.save()

        configPerProjectAndSeqType.project = otherProject
        configPerProjectAndSeqType.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testConfigPerProjectAndSeqTypeIsNullOtherSeqType() {

        configPerProjectAndSeqType.seqType = testData.exomeSeqType
        configPerProjectAndSeqType.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForSnvProcessingAlreadyInProcess() {
        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                config: configPerProjectAndSeqType,
                tumorBamFile: processedMergedBamFile1,
                controlBamFile: processedMergedBamFile2
                )
        snvCallingInstance.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForSnvProcessingWasProcessed() {
        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                config: configPerProjectAndSeqType,
                tumorBamFile: processedMergedBamFile1,
                controlBamFile: processedMergedBamFile2,
                processingState: SnvProcessingStates.FINISHED
                )
        snvCallingInstance.save()

        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForSnvProcessingHasToBeIgnored() {
        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                config: configPerProjectAndSeqType,
                tumorBamFile: processedMergedBamFile1,
                controlBamFile: processedMergedBamFile2,
                processingState: SnvProcessingStates.IGNORED
                )
        snvCallingInstance.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testOtherSamplePairForSnvProcessingInProcess() {
        ProcessedMergedBamFile otherProcessedMergedBamFile = createProcessedMergedBamFile("3")
        otherProcessedMergedBamFile.save()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                config: configPerProjectAndSeqType,
                tumorBamFile: processedMergedBamFile1,
                controlBamFile: otherProcessedMergedBamFile
                )
        snvCallingInstance.save()

        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForOtherIndividualInProcess() {
        Individual otherIndividual = testData.createIndividual([pid: "otherIndividual"])
        otherIndividual.save()

        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile("3")
        processedMergedBamFile3.sample.individual = otherIndividual
        processedMergedBamFile3.sample.sampleType = processedMergedBamFile1.sample.sampleType
        processedMergedBamFile3.save()
        ProcessedMergedBamFile processedMergedBamFile4 = createProcessedMergedBamFile("4")
        processedMergedBamFile4.sample.individual = otherIndividual
        processedMergedBamFile4.sample.sampleType = processedMergedBamFile2.sample.sampleType
        processedMergedBamFile4.save()

        assertEquals(processedMergedBamFile1.sample.sampleType, processedMergedBamFile3.sample.sampleType)
        assertEquals(processedMergedBamFile2.sample.sampleType, processedMergedBamFile4.sample.sampleType)
        assertEquals(processedMergedBamFile1.individual, processedMergedBamFile2.individual)
        assertEquals(processedMergedBamFile3.individual, processedMergedBamFile4.individual)
        assertFalse(processedMergedBamFile1.individual == processedMergedBamFile3.individual)
        assertFalse(processedMergedBamFile2.individual == processedMergedBamFile4.individual)

        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual2 = new SampleTypeCombinationPerIndividual(
                individual: otherIndividual,
                sampleType1: processedMergedBamFile3.sample.sampleType,
                sampleType2: processedMergedBamFile4.sample.sampleType,
                seqType: seqType
                )
        sampleTypeCombinationPerIndividual2.save()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                config: configPerProjectAndSeqType,
                tumorBamFile: processedMergedBamFile3,
                controlBamFile: processedMergedBamFile4
                )
        snvCallingInstance.save()

        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForOtherSeqTypeInProcess() {
        SeqType otherSeqType = testData.exomeSeqType

        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile("3")
        processedMergedBamFile3.sample.sampleType = processedMergedBamFile1.sample.sampleType
        processedMergedBamFile3.save()
        processedMergedBamFile3.mergingWorkPackage.seqType = otherSeqType
        processedMergedBamFile3.mergingWorkPackage.save()
        processedMergedBamFile3.getContainedSeqTracks().each {
            it.seqType = otherSeqType
            it.save()
        }
        ProcessedMergedBamFile processedMergedBamFile4 = createProcessedMergedBamFile("4")
        processedMergedBamFile4.sample.sampleType = processedMergedBamFile2.sample.sampleType
        processedMergedBamFile4.save()
        processedMergedBamFile4.mergingWorkPackage.seqType = otherSeqType
        processedMergedBamFile4.mergingWorkPackage.save()
        processedMergedBamFile4.getContainedSeqTracks().each {
            it.seqType = otherSeqType
            it.save()
        }

        assertEquals(processedMergedBamFile1.sample.sampleType, processedMergedBamFile3.sample.sampleType)
        assertEquals(processedMergedBamFile2.sample.sampleType, processedMergedBamFile4.sample.sampleType)
        assertEquals(processedMergedBamFile1.seqType, processedMergedBamFile2.seqType)
        assertEquals(processedMergedBamFile3.seqType, processedMergedBamFile4.seqType)
        assertFalse(processedMergedBamFile1.seqType == processedMergedBamFile3.seqType)
        assertFalse(processedMergedBamFile2.seqType == processedMergedBamFile4.seqType)


        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual2 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: processedMergedBamFile3.sample.sampleType,
                sampleType2: processedMergedBamFile4.sample.sampleType,
                seqType: otherSeqType
                )
        sampleTypeCombinationPerIndividual2.save()

        SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                config: configPerProjectAndSeqType,
                tumorBamFile: processedMergedBamFile3,
                controlBamFile: processedMergedBamFile4
                )
        snvCallingInstance.save()

        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1DoesNotContainAllSeqTracks() {
        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())

        SeqTrack newSeqTrack = testData.createSeqTrack([
            sample: processedMergedBamFile1.sample,
            seqType: processedMergedBamFile1.seqType]
        )
        newSeqTrack.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2DoesNotContainAllSeqTracks() {
        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())

        SeqTrack newSeqTrack = testData.createSeqTrack([
            sample: processedMergedBamFile2.sample,
            seqType: processedMergedBamFile2.seqType]
        )
        newSeqTrack.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1HasOtherSampleType() {
        SampleType differentSampleType = testData.createSampleType([name: "DIFFERENT"])
        differentSampleType.save()

        sampleTypeCombinationPerIndividual.sampleType1 = differentSampleType
        sampleTypeCombinationPerIndividual.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2HasOtherSampleType() {
        SampleType differentSampleType = testData.createSampleType([name: "DIFFERENT"])
        differentSampleType.save()

        sampleTypeCombinationPerIndividual.sampleType2 = differentSampleType
        sampleTypeCombinationPerIndividual.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFilesHaveOtherSeqType() {
        SeqType differentSeqType = testData.createSeqType([name: "DIFFERENT"])
        differentSeqType.save()

        sampleTypeCombinationPerIndividual.seqType = differentSeqType
        sampleTypeCombinationPerIndividual.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairFromOtherIndividual() {
        Individual otherIndividual = testData.createIndividual([project: project, pid: "testPid2"])
        otherIndividual.save()
        sampleTypeCombinationPerIndividual.individual = otherIndividual
        sampleTypeCombinationPerIndividual.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1StillInProcessing() {
        processedMergedBamFile1.md5sum = null
        processedMergedBamFile1.fileOperationStatus = FileOperationStatus.INPROGRESS
        processedMergedBamFile1.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2StillInProcessing() {
        processedMergedBamFile2.md5sum = null
        processedMergedBamFile2.fileOperationStatus = FileOperationStatus.INPROGRESS
        processedMergedBamFile2.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1CoverageTooLow() {
        processedMergedBamFile1.coverage = COVERAGE_TOO_LOW
        processedMergedBamFile1.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1NumberOfMergedLanesTooLow() {
        processedMergedBamFile1.numberOfMergedLanes = TOO_LITTLE_LANES
        processedMergedBamFile1.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2CoverageTooLow() {
        processedMergedBamFile2.coverage = COVERAGE_TOO_LOW
        processedMergedBamFile2.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2NumberOfMergedLanesTooLow() {
        processedMergedBamFile2.numberOfMergedLanes = TOO_LITTLE_LANES
        processedMergedBamFile2.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBothBamFilesCoverageTooLow() {
        processedMergedBamFile2.coverage = COVERAGE_TOO_LOW
        processedMergedBamFile2.save()

        processedMergedBamFile1.coverage = COVERAGE_TOO_LOW
        processedMergedBamFile1.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBothBamFilesNumberOfMergedLanesTooLow() {
        processedMergedBamFile2.numberOfMergedLanes = TOO_LITTLE_LANES
        processedMergedBamFile2.save()

        processedMergedBamFile1.numberOfMergedLanes = TOO_LITTLE_LANES
        processedMergedBamFile1.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1NoProcessingThresholdWrongProject() {
        Project otherProject = testData.project

        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.project = otherProject
        processingThreshold.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1NoProcessingThresholdWrongSeqType() {
        SeqType otherSeqType = testData.exomeSeqType

        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.seqType = otherSeqType
        processingThreshold.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1NoProcessingThresholdWrongSampleType() {
        SampleType otherSampleType = testData.sampleType

        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.sampleType = otherSampleType
        processingThreshold.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1ProcessingThresholdCoverageIsNull() {
        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.coverage = null
        processingThreshold.save()

        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1ProcessingThresholdNumberOfLanesIsNull() {
        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.numberOfLanes = null
        processingThreshold.save()

        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1DoesNotExist() {
        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())

        processedMergedBamFile1.fileExists = false
        processedMergedBamFile1.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2DoesNotExist() {
        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())

        processedMergedBamFile2.fileExists = false
        processedMergedBamFile2.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1IsWithdrawn() {
        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())

        processedMergedBamFile1.withdrawn = true
        processedMergedBamFile1.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2IsWithdrawn() {
        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())

        processedMergedBamFile2.withdrawn = true
        processedMergedBamFile2.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }


    @Test
    void testIfOrderOfPairsIsCorrect() {
        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile("3")
        processedMergedBamFile3.save()

        ProcessedMergedBamFile processedMergedBamFile4 = createProcessedMergedBamFile("4")
        processedMergedBamFile4.save()

        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual1 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: processedMergedBamFile3.sample.sampleType,
                sampleType2: processedMergedBamFile4.sample.sampleType,
                seqType: seqType
                )
        sampleTypeCombinationPerIndividual1.save()

        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())

        ProcessedMergedBamFile processedMergedBamFile5 = createProcessedMergedBamFile("5")
        processedMergedBamFile5.save()

        ProcessedMergedBamFile processedMergedBamFile6 = createProcessedMergedBamFile("6")
        processedMergedBamFile6.save()

        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual2 = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: processedMergedBamFile5.sample.sampleType,
                sampleType2: processedMergedBamFile6.sample.sampleType,
                seqType: seqType
                )
        sampleTypeCombinationPerIndividual2.save()

        assertEquals(sampleTypeCombinationPerIndividual, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testCheckIfAllAvailableSeqTracksAreIncludedAllIncluded() {
        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile("3")
        processedMergedBamFile3.save()

        assertTrue(snvCallingService.checkIfAllAvailableSeqTracksAreIncluded(processedMergedBamFile3))
    }

    @Test
    void testCheckIfAllAvailableSeqTracksAreIncludedOneMissing() {
        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile("3")
        processedMergedBamFile3.save()

        Sample sample = processedMergedBamFile3.sample
        SeqTrack seqTrack = testData.createSeqTrack([sample: sample, seqType: seqType])
        seqTrack.save()

        assertFalse(snvCallingService.checkIfAllAvailableSeqTracksAreIncluded(processedMergedBamFile3))
    }

    @Test
    void testCheckIfAllAvailableSeqTracksAreIncludedSeqTrackIsWithdrawn() {
        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile("3")
        processedMergedBamFile3.save()

        Sample sample = processedMergedBamFile3.sample
        SeqTrack seqTrack = testData.createSeqTrack([sample: sample, seqType: seqType, laneId: "456"])
        seqTrack.save()

        DataFile dataFile = testData.createDataFile([seqTrack: seqTrack, fileWithdrawn: true])
        dataFile.save()

        assertTrue(snvCallingService.checkIfAllAvailableSeqTracksAreIncluded(processedMergedBamFile3))
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(String identifier) {
        SampleType sampleType = testData.createSampleType([name: "SampleType"+identifier])
        sampleType.save()

        Sample sample = testData.createSample([individual: individual, sampleType: sampleType])
        sample.save()

        SeqTrack seqTrack = testData.createSeqTrack([sample: sample, seqType: seqType])
        seqTrack.save()

        AlignmentPass alignmentPass = testData.createAlignmentPass([seqTrack: seqTrack])
        alignmentPass.save()

        ProcessedBamFile processedBamFile = testData.createProcessedBamFile([alignmentPass: alignmentPass,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED])
        processedBamFile.save()

        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage([sample: sample, seqType: seqType])
        mergingWorkPackage.save()

        MergingSet mergingSet = testData.createMergingSet([mergingWorkPackage: mergingWorkPackage, status: State.PROCESSED])
        mergingSet.save()

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                mergingSet: mergingSet,
                bamFile: processedBamFile
                )
        mergingSetAssignment.save()

        MergingPass mergingPass = testData.createMergingPass([mergingSet: mergingSet])
        mergingPass.save()

        ProcessingThresholds processingThresholds1 = new ProcessingThresholds(
                project: project,
                seqType: seqType,
                sampleType: sampleType,
                coverage: COVERAGE_THRESHOLD,
                numberOfLanes: LANE_THRESHOLD
                )
        processingThresholds1.save()

        return  testData.createProcessedMergedBamFile([
            mergingPass: mergingPass,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            fileOperationStatus: FileOperationStatus.PROCESSED,
            fileExists: true
        ])
    }
}
