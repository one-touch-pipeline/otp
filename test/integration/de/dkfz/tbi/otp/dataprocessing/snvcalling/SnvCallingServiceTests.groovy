package de.dkfz.tbi.otp.dataprocessing.snvcalling

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
        testData.createSnvObjects()

        samplePair = testData.samplePair
        project = samplePair.project
        seqType = samplePair.seqType

        snvConfig = testData.createSnvConfig()

        processedMergedBamFile1 = testData.bamFileTumor
        processedMergedBamFile2 = testData.bamFileControl

        [processedMergedBamFile1, processedMergedBamFile2].each {
            setThresholds(it)
            it.workPackage.bamFileInProjectFolder = it
            assert it.workPackage.save(flush: true)
        }
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

        snvConfig.seqType = DomainFactory.createExomeSeqType()
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
        SamplePair samplePair2 = DomainFactory.createSamplePair(
                processedMergedBamFile1.mergingWorkPackage,
                DomainFactory.createMergingWorkPackage(processedMergedBamFile2.mergingWorkPackage),
                )
        SnvCallingInstance snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: ARBITRARY_INSTANCE_NAME,
                samplePair: samplePair2,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: createProcessedMergedBamFile(samplePair2.mergingWorkPackage2),
                )
        snvCallingInstance.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testSamplePairForOtherIndividualInProcess() {
        Individual otherIndividual = Individual.build()

        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(processedMergedBamFile1.mergingWorkPackage,
                        [sample: Sample.build(individual: otherIndividual, sampleType: processedMergedBamFile1.sampleType)])
        )
        ProcessedMergedBamFile processedMergedBamFile4 = createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(processedMergedBamFile2.mergingWorkPackage,
                        [sample: Sample.build(individual: otherIndividual, sampleType: processedMergedBamFile2.sampleType)])
        )

        assertEquals(processedMergedBamFile1.sample.sampleType, processedMergedBamFile3.sample.sampleType)
        assertEquals(processedMergedBamFile2.sample.sampleType, processedMergedBamFile4.sample.sampleType)
        assertEquals(processedMergedBamFile1.individual, processedMergedBamFile2.individual)
        assertEquals(processedMergedBamFile3.individual, processedMergedBamFile4.individual)
        assertFalse(processedMergedBamFile1.individual == processedMergedBamFile3.individual)
        assertFalse(processedMergedBamFile2.individual == processedMergedBamFile4.individual)

        SampleTypePerProject.build(project: otherIndividual.project, sampleType: processedMergedBamFile3.sample.sampleType, category: SampleType.Category.DISEASE)

        SamplePair samplePair2 = DomainFactory.createSamplePair(
                processedMergedBamFile3.mergingWorkPackage, processedMergedBamFile4.mergingWorkPackage)

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
        SeqType otherSeqType = DomainFactory.createExomeSeqType()

        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(processedMergedBamFile1.mergingWorkPackage, [seqType: otherSeqType]))

        ProcessedMergedBamFile processedMergedBamFile4 = createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(processedMergedBamFile2.mergingWorkPackage, [seqType: otherSeqType]))

        assertEquals(processedMergedBamFile1.sample.sampleType, processedMergedBamFile3.sample.sampleType)
        assertEquals(processedMergedBamFile2.sample.sampleType, processedMergedBamFile4.sample.sampleType)
        assertEquals(processedMergedBamFile1.seqType, processedMergedBamFile2.seqType)
        assertEquals(processedMergedBamFile3.seqType, processedMergedBamFile4.seqType)
        assertFalse(processedMergedBamFile1.seqType == processedMergedBamFile3.seqType)
        assertFalse(processedMergedBamFile2.seqType == processedMergedBamFile4.seqType)

        SamplePair samplePair2 = DomainFactory.createSamplePair(
                processedMergedBamFile3.mergingWorkPackage, processedMergedBamFile4.mergingWorkPackage)

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

        DomainFactory.buildSeqTrackWithDataFile(processedMergedBamFile1.mergingWorkPackage)

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile2DoesNotContainAllSeqTracks() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())

        DomainFactory.buildSeqTrackWithDataFile(processedMergedBamFile2.mergingWorkPackage)

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testNoSamplePairForBamFile1() {
        MergingWorkPackage otherMwp = DomainFactory.createMergingWorkPackage(samplePair.mergingWorkPackage1)

        SampleTypePerProject.build(project: project, sampleType: otherMwp.sampleType, category: SampleType.Category.DISEASE)

        DomainFactory.createSamplePair(otherMwp, samplePair.mergingWorkPackage2)
        samplePair.delete()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testNoSamplePairForBamFile2() {
        MergingWorkPackage otherMwp = DomainFactory.createMergingWorkPackage(samplePair.mergingWorkPackage2)

        DomainFactory.createSamplePair(samplePair.mergingWorkPackage1, otherMwp)
        samplePair.delete()

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
        Project otherProject = Project.build()

        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.project = otherProject
        processingThreshold.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1NoProcessingThresholdWrongSeqType() {
        SeqType otherSeqType = DomainFactory.createExomeSeqType()

        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.seqType = otherSeqType
        processingThreshold.save()

        assertNull(snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testBamFile1NoProcessingThresholdWrongSampleType() {
        SampleType otherSampleType = SampleType.build()

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

        def createAnotherProcessableSamplePair = {
            ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile(
                    DomainFactory.createMergingWorkPackage(processedMergedBamFile1.mergingWorkPackage))
            ProcessedMergedBamFile processedMergedBamFile4 = createProcessedMergedBamFile(
                    DomainFactory.createMergingWorkPackage(processedMergedBamFile1.mergingWorkPackage))

            SampleTypePerProject.build(project: project, sampleType: processedMergedBamFile3.sample.sampleType, category: SampleType.Category.DISEASE)

            DomainFactory.createSamplePair(processedMergedBamFile3.mergingWorkPackage, processedMergedBamFile4.mergingWorkPackage)
        }

        createAnotherProcessableSamplePair()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())

        createAnotherProcessableSamplePair()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing())
    }

    @Test
    void testCheckIfAllAvailableSeqTracksAreIncludedAllIncluded() {
        assertTrue(snvCallingService.checkIfAllAvailableSeqTracksAreIncluded(processedMergedBamFile1))
    }

    @Test
    void testCheckIfAllAvailableSeqTracksAreIncludedOneMissing() {
        DomainFactory.buildSeqTrackWithDataFile(processedMergedBamFile1.mergingWorkPackage)

        assertFalse(snvCallingService.checkIfAllAvailableSeqTracksAreIncluded(processedMergedBamFile1))
    }

    @Test
    void testCheckIfAllAvailableSeqTracksAreIncludedSeqTrackIsWithdrawn() {
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(processedMergedBamFile1.mergingWorkPackage)
        DataFile dataFile = DataFile.findBySeqTrack(seqTrack)
        dataFile.fileWithdrawn = true
        assert dataFile.save(failOnError: true)

        assertTrue(snvCallingService.checkIfAllAvailableSeqTracksAreIncluded(processedMergedBamFile1))
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        final ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, properties + DomainFactory.PROCESSED_BAM_FILE_PROPERTIES)
        setThresholds(bamFile)
        return bamFile
    }

    private void setThresholds(ProcessedMergedBamFile bamFile) {
        bamFile.coverage = COVERAGE_THRESHOLD
        bamFile.numberOfMergedLanes = LANE_THRESHOLD
        DomainFactory.assignNewProcessedBamFile(bamFile.mergingSet)
        DomainFactory.assignNewProcessedBamFile(bamFile.mergingSet)
        assert bamFile.save(failOnError: true)

        ProcessingThresholds processingThresholds1 = new ProcessingThresholds(
                project: bamFile.project,
                seqType: bamFile.seqType,
                sampleType: bamFile.sampleType,
                coverage: COVERAGE_THRESHOLD,
                numberOfLanes: LANE_THRESHOLD
        )
        assert processingThresholds1.save(failOnError: true)
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
