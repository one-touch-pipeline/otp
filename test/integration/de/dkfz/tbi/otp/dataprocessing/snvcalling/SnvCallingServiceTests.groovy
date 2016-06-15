package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Before
import org.junit.Test

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static org.junit.Assert.*

class SnvCallingServiceTests {

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
            adaptThresholdAndBamFileInProjectFolderProperty(it)
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
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testSamplePairNoProcessingNeeded() {
        samplePair.processingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair.save(flush: true)
        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testSamplePairDisabled() {
        samplePair.processingStatus = ProcessingStatus.DISABLED
        assert samplePair.save(flush: true)
        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testSnvConfigIsNullOtherProject() {
        Project otherProject = TestData.createProject(name: "otherProject", dirName: "tmp", realmName: "DKFZ")
        assert otherProject.save(flush: true)

        snvConfig.project = otherProject
        assert snvConfig.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testSnvConfigIsNullOtherSeqType() {

        snvConfig.seqType = DomainFactory.createExomeSeqType()
        snvConfig.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testSamplePairForSnvProcessing_NoCorrespondingExternalScriptInDatabase_shouldReturnNull() {
        testData.externalScript_Joining.scriptVersion = "v2"
        assert testData.externalScript_Joining.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
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

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
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

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testSamplePairForSnvProcessingHasFailed() {
        SnvCallingInstance snvCallingInstance = createAndSaveSnvCallingInstanceAndSnvJobResults()
        snvCallingService.markSnvCallingInstanceAsFailed(snvCallingInstance, [SnvCallingStep.SNV_ANNOTATION])
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
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

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
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

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testSamplePairForOtherSeqTypeInProcess() {
        SeqType otherSeqType = DomainFactory.createExomeSeqType()
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()

        ProcessedMergedBamFile processedMergedBamFile3 = createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(processedMergedBamFile1.mergingWorkPackage, [seqType: otherSeqType, libraryPreparationKit: libraryPreparationKit]))

        ProcessedMergedBamFile processedMergedBamFile4 = createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(processedMergedBamFile2.mergingWorkPackage, [seqType: otherSeqType, libraryPreparationKit: libraryPreparationKit]))


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

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1DoesNotContainAllSeqTracks() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))

        DomainFactory.createSeqTrackWithDataFiles(processedMergedBamFile1.mergingWorkPackage)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile2DoesNotContainAllSeqTracks() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))

        DomainFactory.createSeqTrackWithDataFiles(processedMergedBamFile2.mergingWorkPackage)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testNoSamplePairForBamFile1() {
        MergingWorkPackage otherMwp = DomainFactory.createMergingWorkPackage(samplePair.mergingWorkPackage1)

        SampleTypePerProject.build(project: project, sampleType: otherMwp.sampleType, category: SampleType.Category.DISEASE)

        DomainFactory.createSamplePair(otherMwp, samplePair.mergingWorkPackage2)
        samplePair.delete(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testNoSamplePairForBamFile2() {
        MergingWorkPackage otherMwp = DomainFactory.createMergingWorkPackage(samplePair.mergingWorkPackage2)

        DomainFactory.createSamplePair(samplePair.mergingWorkPackage1, otherMwp)
        samplePair.delete(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1StillInProcessing() {
        processedMergedBamFile1.md5sum = null
        processedMergedBamFile1.fileOperationStatus = FileOperationStatus.INPROGRESS
        assert processedMergedBamFile1.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile2StillInProcessing() {
        processedMergedBamFile2.md5sum = null
        processedMergedBamFile2.fileOperationStatus = FileOperationStatus.INPROGRESS
        assert processedMergedBamFile2.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1CoverageTooLow() {
        processedMergedBamFile1.coverage = COVERAGE_TOO_LOW
        assert processedMergedBamFile1.save(flush: true)
        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1NumberOfMergedLanesTooLow() {
        processedMergedBamFile1.numberOfMergedLanes = TOO_LITTLE_LANES
        assert processedMergedBamFile1.save(flush: true)
        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile2CoverageTooLow() {
        processedMergedBamFile2.coverage = COVERAGE_TOO_LOW
        assert processedMergedBamFile2.save(flush: true)
        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile2NumberOfMergedLanesTooLow() {
        processedMergedBamFile2.numberOfMergedLanes = TOO_LITTLE_LANES
        assert processedMergedBamFile2.save(flush: true)
        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBothBamFilesCoverageTooLow() {
        processedMergedBamFile2.coverage = COVERAGE_TOO_LOW
        assert processedMergedBamFile2.save(flush: true)

        processedMergedBamFile1.coverage = COVERAGE_TOO_LOW
        assert processedMergedBamFile1.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBothBamFilesNumberOfMergedLanesTooLow() {
        processedMergedBamFile2.numberOfMergedLanes = TOO_LITTLE_LANES
        assert processedMergedBamFile2.save(flush: true)

        processedMergedBamFile1.numberOfMergedLanes = TOO_LITTLE_LANES
        assert processedMergedBamFile1.save(flush: true)
        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1NoProcessingThresholdWrongProject() {
        Project otherProject = Project.build()

        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.project = otherProject
        assert processingThreshold.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1NoProcessingThresholdWrongSeqType() {
        SeqType otherSeqType = DomainFactory.createExomeSeqType()

        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.seqType = otherSeqType
        assert processingThreshold.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1NoProcessingThresholdWrongSampleType() {
        SampleType otherSampleType = SampleType.build()

        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.sampleType = otherSampleType
        assert processingThreshold.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1ProcessingThresholdCoverageIsNull() {
        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.coverage = null
        assert processingThreshold.save(flush: true)

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile1ProcessingThresholdNumberOfLanesIsNull() {
        ProcessingThresholds processingThreshold = exactlyOneElement(ProcessingThresholds.findAllByProjectAndSeqTypeAndSampleType(
                processedMergedBamFile1.project, processedMergedBamFile1.seqType, processedMergedBamFile1.sample.sampleType
                ))
        processingThreshold.numberOfLanes = null
        processingThreshold.save()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }


    @Test
    void testBamFile1IsWithdrawn() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))

        processedMergedBamFile1.withdrawn = true
        assert processedMergedBamFile1.save(flush: true)

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testBamFile2IsWithdrawn() {
        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))

        processedMergedBamFile2.withdrawn = true
        processedMergedBamFile2.save()

        assertNull(snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
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

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))

        createAnotherProcessableSamplePair()

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testIfFastTrackIsProcessedFirst() {
        testData.externalScript_Joining.delete()

        testData.createSnvObjects()

        SamplePair samplePair2 = testData.samplePair
        Project project2 = samplePair2.project
        project2.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert project2.save(flush: true)

        testData.createSnvConfig()


        [testData.bamFileTumor, testData.bamFileControl].each {
            adaptThresholdAndBamFileInProjectFolderProperty(it)
        }

        assertEquals(samplePair2, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.NORMAL_PRIORITY))

    }

    @Test
    void testThatMinProcessingPriorityIsTakenIntoAccount() {
        assertNull (snvCallingService.samplePairForSnvProcessing(ProcessingPriority.FAST_TRACK_PRIORITY))

        project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert project.save(flush: true)

        assertEquals(samplePair, snvCallingService.samplePairForSnvProcessing(ProcessingPriority.FAST_TRACK_PRIORITY))
    }


    private ProcessedMergedBamFile createProcessedMergedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        final ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, properties + DomainFactory.randomProcessedBamFileProperties)
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

    private void adaptThresholdAndBamFileInProjectFolderProperty(ProcessedMergedBamFile processedMergedBamFile) {
        setThresholds(processedMergedBamFile)
        processedMergedBamFile.workPackage.bamFileInProjectFolder = processedMergedBamFile
        assert processedMergedBamFile.workPackage.save(flush: true)
    }
}
