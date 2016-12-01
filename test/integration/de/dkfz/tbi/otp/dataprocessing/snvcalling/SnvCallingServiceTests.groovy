package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

class SnvCallingServiceTests {

    SnvCallingInstanceTestData testData
    SamplePair samplePair

    SnvCallingService snvCallingService
    SnvConfig snvConfig
    RoddyWorkflowConfig roddyConfig
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
        roddyConfig = DomainFactory.createRoddyWorkflowConfig(
                project: samplePair.project,
                seqType: samplePair.seqType,
                pipeline: DomainFactory.createRoddySnvPipelineLazy()
        )

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
        roddyConfig = null
        processedMergedBamFile1 = null
        processedMergedBamFile2 = null
        project = null
        individual = null
        seqType = null
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
        assert instance.withdrawn == true
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
                processingState: AnalysisProcessingStates.IN_PROGRESS
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


    @Test
    void testGetLatestValidJobResultForStep_PreviousResultsIsValid_AllFine() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        SnvCallingInstance instance = DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance)

        assert snvJobResult == snvCallingService.getLatestValidJobResultForStep(instance, snvJobResult.step)
    }

    @Test
    void testGetLatestValidJobResultForStep_PreviousPreviousResultsIsInValid_AllFine() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        DomainFactory.createSnvJobResult(
                step: snvJobResult.step,
                withdrawn: true,
                externalScript: snvJobResult.externalScript,
                chromosomeJoinExternalScript: snvJobResult.chromosomeJoinExternalScript,
                snvCallingInstance: DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance),

        )
        SnvCallingInstance instance = DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance)

        assert snvJobResult == snvCallingService.getLatestValidJobResultForStep(instance, snvJobResult.step)
    }

    @Test
    void testGetLatestValidJobResultForStep_NoPreviousInstanceExist_ShouldFail() {
        SnvCallingInstance instance = DomainFactory.createSnvInstanceWithRoddyBamFiles()

        TestCase.shouldFailWithMessageContaining(AssertionError, 'There is no valid previous result file for sample pair') {
            snvCallingService.getLatestValidJobResultForStep(instance, SnvCallingStep.CALLING)
        }
    }

    @Test
    void testGetLatestValidJobResultForStep_PreviousResultIsWithdrawn_ShouldFail() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles([withdrawn: true])
        SnvCallingInstance instance = DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance)

        TestCase.shouldFailWithMessageContaining(AssertionError, 'There is no valid previous result file for sample pair') {
            snvCallingService.getLatestValidJobResultForStep(instance, SnvCallingStep.CALLING)
        }
    }

    @Test
    void testGetLatestValidJobResultForStep_PreviousResultIsFailed_ShouldFail() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles([processingState: AnalysisProcessingStates.IN_PROGRESS])
        SnvCallingInstance instance = DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance)

        TestCase.shouldFailWithMessageContaining(AssertionError, 'There is no valid previous result file for sample pair') {
            snvCallingService.getLatestValidJobResultForStep(instance, SnvCallingStep.CALLING)
        }
    }

    @Test
    void testGetLatestValidJobResultForStep_BamFile1Changed_ShouldFail() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        SnvCallingInstance instance = DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance, [
                sampleType1BamFile: DomainFactory.createRoddyBamFile([
                        workPackage: snvJobResult.sampleType1BamFile.mergingWorkPackage,
                        config: snvJobResult.sampleType1BamFile.config,
                ])
        ])

        TestCase.shouldFailWithMessageContaining(AssertionError, 'The first bam file has changed between instance') {
            snvCallingService.getLatestValidJobResultForStep(instance, SnvCallingStep.CALLING)
        }
    }

    @Test
    void testGetLatestValidJobResultForStep_BamFile2Changed_ShouldFail() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        SnvCallingInstance instance = DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance, [
                sampleType2BamFile: DomainFactory.createRoddyBamFile([
                        workPackage: snvJobResult.sampleType2BamFile.mergingWorkPackage,
                        config: snvJobResult.sampleType2BamFile.config,
                ])
        ])

        TestCase.shouldFailWithMessageContaining(AssertionError, 'The second bam file has changed between instance') {
            snvCallingService.getLatestValidJobResultForStep(instance, SnvCallingStep.CALLING)
        }
    }

    @Test
    void testGetLatestValidJobResultForStep_BamFile1Withdrawn_ShouldFail() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        SnvCallingInstance instance = DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance)
        snvJobResult.sampleType1BamFile.withdrawn = true
        assert snvJobResult.sampleType1BamFile.save(flush: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, 'of the previous result is withdrawn') {
            snvCallingService.getLatestValidJobResultForStep(instance, SnvCallingStep.CALLING)
        }
    }

    @Test
    void testGetLatestValidJobResultForStep_BamFile2Withdrawn_ShouldFail() {
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        SnvCallingInstance instance = DomainFactory.createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(snvJobResult.snvCallingInstance)
        snvJobResult.sampleType2BamFile.withdrawn = true
        assert snvJobResult.sampleType2BamFile.save(flush: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, 'of the previous result is withdrawn') {
            snvCallingService.getLatestValidJobResultForStep(instance, SnvCallingStep.CALLING)
        }
    }
}
