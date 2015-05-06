package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.job.jobs.snvcalling.SnvCallingJob
import de.dkfz.tbi.otp.utils.ExternalScript

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import static de.dkfz.tbi.otp.utils.CollectionUtils.*


class SnvCallingServiceTests extends GroovyTestCase {

    SnvCallingInstanceTestData testData
    SamplePair samplePair

    SnvCallingService snvCallingService
    SnvConfig snvConfig
    ProcessedMergedBamFile processedMergedBamFile1
    ProcessedMergedBamFile processedMergedBamFile2
    Project project
    Individual individual
    SeqType seqType

    final static String ARBITRARY_INSTANCE_NAME = '2014-08-25_15h32'

    final static double COVERAGE_THRESHOLD = 30.0
    final static double COVERAGE_TOO_LOW = 20.0

    final static int LANE_THRESHOLD = 3
    final static int TOO_LITTLE_LANES = 2

    @Before
    void setUp() {
        testData = new SnvCallingInstanceTestData()
        testData.createObjects()

        project = testData.createProject()
        project.save()

        individual = testData.createIndividual([project: project, pid: "testPid"])
        individual.save()

        seqType = testData.createSeqType([name: 'TEST_SEQTYPE', dirName: 'test_seqtype'])
        assert seqType.save()

        snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: "configuration",
                externalScriptVersion: "v1",
                )
        assert snvConfig.save()

        processedMergedBamFile1 = createProcessedMergedBamFile("1")
        processedMergedBamFile1.save(flush: true)

        processedMergedBamFile2 = createProcessedMergedBamFile("2")
        processedMergedBamFile2.save(flush: true)

        SampleTypePerProject.build(project: project, sampleType: processedMergedBamFile1.sample.sampleType, category: SampleType.Category.DISEASE)

        samplePair = new SamplePair(
                individual: individual,
                sampleType1: processedMergedBamFile1.sample.sampleType,
                sampleType2: processedMergedBamFile2.sample.sampleType,
                seqType: seqType
                )
        samplePair.save(flush: true)

        testData.externalScript_Joining = new ExternalScript(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/joining.sh",
                author: "otptest",
        )
        assert testData.externalScript_Joining.save()
    }

    @After
    void tearDown() {
        samplePair = null
        snvConfig = null
        processedMergedBamFile1 = null
        processedMergedBamFile2 = null
        project = null
        individual = null
        seqType = null
    }

    @Test
    void testSamplePairForSnvProcessingAllCorrect() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairNoProcessingNeeded() {
        samplePair.processingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairDisabled() {
        samplePair.processingStatus = ProcessingStatus.DISABLED
        assert samplePair.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSnvConfigIsNullOtherProject() {
        Project otherProject = TestData.createProject(name: "otherProject", dirName: "/tmp", realmName: "DKFZ")
        otherProject.save()

        snvConfig.project = otherProject
        snvConfig.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSnvConfigIsNullOtherSeqType() {

        snvConfig.seqType = testData.exomeSeqType
        snvConfig.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForSnvProcessing_NoCorrespondingExternalScriptInDatabase_shouldReturnNull() {
        testData.externalScript_Joining.scriptVersion = "v2"
        assert testData.externalScript_Joining.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }


    @Test
    void testSamplePairForSnvProcessingAlreadyInProcess() {
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2
                )
        snvCallingInstance.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForSnvProcessingWasProcessed() {
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                processingState: SnvProcessingStates.FINISHED
                )
        snvCallingInstance.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForSnvProcessingHasFailed() {
        SnvCallingInstance snvCallingInstance = createAndSaveSnvCallingInstanceAndSnvJobResults()
        snvCallingService.markSnvCallingInstanceAsFailed(snvCallingInstance, [SnvCallingStep.SNV_ANNOTATION])
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testOtherSamplePairForSnvProcessingInProcess() {
        ProcessedMergedBamFile otherProcessedMergedBamFile = createProcessedMergedBamFile("3")
        otherProcessedMergedBamFile.save()

        SamplePair samplePair2 = new SamplePair(
                individual: individual,
                sampleType1: processedMergedBamFile1.sample.sampleType,
                sampleType2: otherProcessedMergedBamFile.sample.sampleType,
                seqType: seqType
                )
        samplePair2.save()

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair2,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: otherProcessedMergedBamFile
                )
        snvCallingInstance.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
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

        SampleTypePerProject.build(project: otherIndividual.project, sampleType: processedMergedBamFile3.sample.sampleType, category: SampleType.Category.DISEASE)

        SamplePair samplePair2 = new SamplePair(
                individual: otherIndividual,
                sampleType1: processedMergedBamFile3.sample.sampleType,
                sampleType2: processedMergedBamFile4.sample.sampleType,
                seqType: seqType
                )
        samplePair2.save()

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair2,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile3,
                sampleType2BamFile: processedMergedBamFile4
                )
        snvCallingInstance.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForOtherSeqTypeInProcess() {
        SeqType otherSeqType = testData.exomeSeqType

        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile("3")
        processedMergedBamFile3.mergingWorkPackage.sample = processedMergedBamFile1.sample
        processedMergedBamFile3.save()

        processedMergedBamFile3.mergingWorkPackage.seqType = otherSeqType
        processedMergedBamFile3.mergingWorkPackage.save()
        processedMergedBamFile3.getContainedSeqTracks().each {
            it.seqType = otherSeqType
            it.save()
        }

        ProcessedMergedBamFile processedMergedBamFile4 = createProcessedMergedBamFile("4")
        processedMergedBamFile4.mergingWorkPackage.sample = processedMergedBamFile2.sample
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

        SamplePair samplePair2 = new SamplePair(
                individual: individual,
                sampleType1: processedMergedBamFile3.sample.sampleType,
                sampleType2: processedMergedBamFile4.sample.sampleType,
                seqType: otherSeqType
                )
        samplePair2.save()

        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair2,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile3,
                sampleType2BamFile: processedMergedBamFile4
                )
        snvCallingInstance.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1DoesNotContainAllSeqTracks() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())

        SeqTrack newSeqTrack = testData.createSeqTrack([
            sample: processedMergedBamFile1.sample,
            seqType: processedMergedBamFile1.seqType]
        )
        newSeqTrack.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2DoesNotContainAllSeqTracks() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())

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

        SampleTypePerProject.build(project: project, sampleType: differentSampleType, category: SampleType.Category.DISEASE)

        samplePair.sampleType1 = differentSampleType
        samplePair.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2HasOtherSampleType() {
        SampleType differentSampleType = testData.createSampleType([name: "DIFFERENT"])
        differentSampleType.save()

        samplePair.sampleType2 = differentSampleType
        samplePair.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFilesHaveOtherSeqType() {
        SeqType differentSeqType = testData.createSeqType([name: "DIFFERENT", dirName: "DIFFERENT_SEQUENCING"])
        differentSeqType.save()

        samplePair.seqType = differentSeqType
        samplePair.save()
        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairFromOtherIndividual() {
        Individual otherIndividual = testData.createIndividual([project: project, pid: "testPid2"])
        otherIndividual.save()
        samplePair.individual = otherIndividual
        samplePair.save()

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

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1ProcessingThresholdNumberOfLanesIsNull() {
        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.numberOfLanes = null
        processingThreshold.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }


    @Test
    void testBamFile1IsWithdrawn() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())

        processedMergedBamFile1.withdrawn = true
        processedMergedBamFile1.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2IsWithdrawn() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())

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

        SampleTypePerProject.build(project: project, sampleType: processedMergedBamFile3.sample.sampleType, category: SampleType.Category.DISEASE)

        SamplePair samplePair1 = new SamplePair(
                individual: individual,
                sampleType1: processedMergedBamFile3.sample.sampleType,
                sampleType2: processedMergedBamFile4.sample.sampleType,
                seqType: seqType
                )
        samplePair1.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())

        ProcessedMergedBamFile processedMergedBamFile5 = createProcessedMergedBamFile("5")
        processedMergedBamFile5.save()

        ProcessedMergedBamFile processedMergedBamFile6 = createProcessedMergedBamFile("6")
        processedMergedBamFile6.save()

        SampleTypePerProject.build(project: project, sampleType: processedMergedBamFile5.sample.sampleType, category: SampleType.Category.DISEASE)

        SamplePair samplePair2 = new SamplePair(
                individual: individual,
                sampleType1: processedMergedBamFile5.sample.sampleType,
                sampleType2: processedMergedBamFile6.sample.sampleType,
                seqType: seqType
                )
        samplePair2.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
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

        final ProcessedMergedBamFile bamFile = testData.createProcessedMergedBamFile(individual, seqType, identifier)
        bamFile.coverage = COVERAGE_THRESHOLD
        bamFile.numberOfMergedLanes = LANE_THRESHOLD
        DomainFactory.assignNewProcessedBamFile(bamFile.mergingSet)
        DomainFactory.assignNewProcessedBamFile(bamFile.mergingSet)
        assert bamFile.save(failOnError: true)

        ProcessingThresholds processingThresholds1 = new ProcessingThresholds(
                project: project,
                seqType: seqType,
                sampleType: bamFile.sampleType,
                coverage: COVERAGE_THRESHOLD,
                numberOfLanes: LANE_THRESHOLD
        )
        assert processingThresholds1.save(failOnError: true)

        return bamFile
    }

    @Test
    void testMarkAsFailed_Correct() {
        def instance = createAndSaveSnvCallingInstanceAndSnvJobResults()
        snvCallingService.markSnvCallingInstanceAsFailed(instance, [SnvCallingStep.SNV_ANNOTATION])
        assert instance.processingState == SnvProcessingStates.FAILED
        def callingResult = SnvJobResult.findAllBySnvCallingInstanceAndStep(instance, SnvCallingStep.CALLING).first()
        def annotationResult = SnvJobResult.findAllBySnvCallingInstanceAndStep(instance, SnvCallingStep.SNV_ANNOTATION).first()
        assert callingResult.withdrawn == false
        assert annotationResult.withdrawn == true
    }

    @Test
    void testMarkAsFailed_NotExistingSnvJobResult() {
        def instance = createAndSaveSnvCallingInstanceAndSnvJobResults()
        shouldFail { snvCallingService.markSnvCallingInstanceAsFailed(instance, [SnvCallingStep.SNV_DEEPANNOTATION]) }
    }

    @Test
    void testMarkAsFailed_WrongInput() {
        shouldFail { snvCallingService.markSnvCallingInstanceAsFailed([]) }
    }

    private def createAndSaveSnvCallingInstanceAndSnvJobResults() {
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                processingState: SnvProcessingStates.IN_PROGRESS
        )
        snvCallingInstance.save(flush: true)
        SnvJobResult callingResult = testData.createAndSaveSnvJobResult(snvCallingInstance, SnvCallingStep.CALLING)
        testData.createAndSaveSnvJobResult(snvCallingInstance, SnvCallingStep.SNV_ANNOTATION, callingResult)
        return  snvCallingInstance
    }
}
