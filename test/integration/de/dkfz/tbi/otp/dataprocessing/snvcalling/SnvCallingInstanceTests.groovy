package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import org.junit.Before
import org.junit.Test

class SnvCallingInstanceTests {

    SnvCallingInstanceTestData testData = new SnvCallingInstanceTestData()

    @Before
    void setUp() {
        testData.createSnvObjects()
    }

    /**
     * This test ensures that "latest" is defined in respect of the {@link SnvCallingInstance}, not in respect of the
     * {@link SnvJobResult}.
     */
    @Test
    void testFindLatestResultForSameBamFiles_correctOrder() {
        final SnvCallingInstance tumor1InstanceA = testData.createAndSaveSnvCallingInstance()
        // Using a different (does not matter if "earlier" or "later") instance name, because instance names have to be unique for the same sample pair.
        final SnvCallingInstance tumor1InstanceB = testData.createAndSaveSnvCallingInstance(instanceName: '2014-09-24_15h04')
        final SnvJobResult tumor1CallingResultB = testData.createAndSaveSnvJobResult(tumor1InstanceB, SnvCallingStep.CALLING)
        final SnvJobResult tumor1CallingResultA = testData.createAndSaveSnvJobResult(tumor1InstanceA, SnvCallingStep.CALLING)

        assert tumor1InstanceA.id < tumor1InstanceB.id
        assert tumor1CallingResultA.id > tumor1CallingResultB.id

        // tumor1CallingResultB should be returned ...
        // - although it has been created after the other result
        // - because it belongs to the instance that has been created later
        // This case will probably not occur in production, because currently the SnvStartJob does not create a new
        // SnvCallingInstance as long as another instance for the same BAM files is in progress.
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.CALLING) == tumor1CallingResultB
        assert tumor1InstanceB.findLatestResultForSameBamFiles(SnvCallingStep.CALLING) == tumor1CallingResultB
    }

    @Test
    void testFindLatestResultForSameBamFiles() {
        final SnvCallingInstance tumor1InstanceA = testData.createAndSaveSnvCallingInstance()
        // Using a different (does not matter if "earlier" or "later") instance name, because instance names have to be unique for the same sample pair.
        final SnvCallingInstance tumor1InstanceB = testData.createAndSaveSnvCallingInstance(instanceName: '2014-09-24_15h04')
        def (ProcessedMergedBamFile bamFileTumor2, SamplePair samplePair2) = testData.createDisease(testData.bamFileControl.mergingWorkPackage)
        final SnvCallingInstance tumor2Instance = testData.createAndSaveSnvCallingInstance(sampleType1BamFile: bamFileTumor2, samplePair: samplePair2)

        // no result at all
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == null

        // result for different step should not be found
        final SnvJobResult tumor1CallingResultA = testData.createAndSaveSnvJobResult(tumor1InstanceA, SnvCallingStep.CALLING)
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == null

        // unfinished result should not be found
        final SnvJobResult tumor1AnnotationResultA = testData.createAndSaveSnvJobResult(tumor1InstanceA, SnvCallingStep.SNV_ANNOTATION, tumor1CallingResultA, SnvProcessingStates.IN_PROGRESS)
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == null

        // withdrawn result should not be found
        tumor1AnnotationResultA.processingState = SnvProcessingStates.FINISHED
        tumor1AnnotationResultA.withdrawn = true
        assert tumor1AnnotationResultA.save(failOnError: true)
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == null

        // result for tumor2 should be found by tumor2Instance only
        final SnvJobResult tumor2CallingResult = testData.createAndSaveSnvJobResult(tumor2Instance, SnvCallingStep.CALLING)
        final SnvJobResult tumor2AnnotationResult = testData.createAndSaveSnvJobResult(tumor2Instance, SnvCallingStep.SNV_ANNOTATION, tumor2CallingResult, SnvProcessingStates.FINISHED)

        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == null
        assert tumor2Instance.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor2AnnotationResult

        // result for tumor1 should be found by tumor1Instances only
        tumor1AnnotationResultA.withdrawn = false
        assert tumor1AnnotationResultA.save(failOnError: true)
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor1AnnotationResultA
        assert tumor1InstanceB.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor1AnnotationResultA
        assert tumor2Instance.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor2AnnotationResult

        // two results for tumor1, instance B is newer
        assert tumor1InstanceA.id < tumor1InstanceB.id
        final SnvJobResult tumor1CallingResultB = testData.createAndSaveSnvJobResult(tumor1InstanceB, SnvCallingStep.CALLING)
        final SnvJobResult tumor1AnnotationResultB = testData.createAndSaveSnvJobResult(tumor1InstanceB, SnvCallingStep.SNV_ANNOTATION, tumor1CallingResultB, SnvProcessingStates.FINISHED)
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor1AnnotationResultB
        assert tumor1InstanceB.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor1AnnotationResultB
        assert tumor2Instance.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor2AnnotationResult

        // result for a different step should not be found
        testData.createAndSaveSnvJobResult(tumor1InstanceA, SnvCallingStep.SNV_DEEPANNOTATION, tumor1AnnotationResultA, SnvProcessingStates.FINISHED)
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor1AnnotationResultB
        assert tumor1InstanceB.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor1AnnotationResultB
        assert tumor2Instance.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor2AnnotationResult

        // withdrawn result should not be found
        tumor1AnnotationResultB.withdrawn = true
        assert tumor1AnnotationResultB.save(failOnError: true)
        assert tumor1InstanceA.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor1AnnotationResultA
        assert tumor1InstanceB.findLatestResultForSameBamFiles(SnvCallingStep.SNV_ANNOTATION) == tumor1AnnotationResultA
    }

    @Test
    void test_updateProcessingState_WhenStateIsNull_ShouldFail() {
        SnvCallingInstance snvCallingInstance = testData.createAndSaveSnvCallingInstance()
        def msg = shouldFail AssertionError, { snvCallingInstance.updateProcessingState(null) }
        assert msg =~ /not allowed to be null/
    }

    @Test
    void test_updateProcessingState_WhenStateIsChangedToSame_ShouldSucceed() {
        SnvCallingInstance snvCallingInstance = testData.createAndSaveSnvCallingInstance([processingState: SnvProcessingStates.IN_PROGRESS])
        snvCallingInstance.updateProcessingState(SnvProcessingStates.IN_PROGRESS)
        assert snvCallingInstance.processingState == SnvProcessingStates.IN_PROGRESS
    }

    @Test
    void test_updateProcessingState_WhenStateIsChangedToDifferent_ShouldSucceed() {
        SnvCallingInstance snvCallingInstance = testData.createAndSaveSnvCallingInstance([processingState: SnvProcessingStates.IN_PROGRESS])
        snvCallingInstance.updateProcessingState(SnvProcessingStates.FINISHED)
        assert snvCallingInstance.processingState == SnvProcessingStates.FINISHED
    }

    private def createSnvCallingInstanceAndSnvJobResults(boolean oneSnvJobResultWithdrawn = false) {
        SnvCallingInstance instance = testData.createAndSaveSnvCallingInstance()
        SnvJobResult snvJobResultCalling = testData.createAndSaveSnvJobResult(instance, SnvCallingStep.CALLING)
        if (oneSnvJobResultWithdrawn) {
            testData.createAndSaveSnvJobResult(instance, SnvCallingStep.SNV_ANNOTATION, snvJobResultCalling, SnvProcessingStates.FINISHED, true)
        } else {
            testData.createAndSaveSnvJobResult(instance, SnvCallingStep.SNV_ANNOTATION, snvJobResultCalling)
        }
        return instance
    }

    @Test
    void testProcessingStateIsFailedAndNoSnvJobResults() {
        def instance = testData.createAndSaveSnvCallingInstance()
        instance.processingState = SnvProcessingStates.FAILED
        assert instance.validate()
    }

    @Test
    void testProcessingStateIsFailedAndNoWithdrawnSnvJobResults() {
        def instance = createSnvCallingInstanceAndSnvJobResults()
        instance.processingState = SnvProcessingStates.FAILED
        assert !instance.validate()
    }

    @Test
    void testProcessingStateIsFailedAndThereIsWithdrawnSnvJobResults() {
        def instance = createSnvCallingInstanceAndSnvJobResults(true)
        instance.processingState = SnvProcessingStates.FAILED
        assert instance.validate()
    }

    @Test
    void testProcessingStateNotFailedAndNoWithdrawnSnvJobResults() {
        def instance = createSnvCallingInstanceAndSnvJobResults()
        assert instance.validate()
    }

    @Test
    void testProcessingStateNotFailedAndThereIsWithdrawnSnvJobResults() {
        def instance = createSnvCallingInstanceAndSnvJobResults(true)
        assert instance.validate()
    }
}
