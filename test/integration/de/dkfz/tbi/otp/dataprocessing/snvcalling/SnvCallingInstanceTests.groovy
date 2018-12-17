package de.dkfz.tbi.otp.dataprocessing.snvcalling

import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class SnvCallingInstanceTests {

    SamplePair samplePair

    @Before
    void setUp() {
        samplePair = DomainFactory.createSamplePairWithProcessedMergedBamFiles()
    }

    @Test
    void test_updateProcessingState_WhenStateIsNull_ShouldFail() {
        RoddySnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        def msg = shouldFail AssertionError, { snvCallingInstance.updateProcessingState(null) }
        assert msg =~ /not allowed to be null/
    }

    @Test
    void test_updateProcessingState_WhenStateIsChangedToSame_ShouldSucceed() {
        RoddySnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        snvCallingInstance.updateProcessingState(AnalysisProcessingStates.IN_PROGRESS)
        assert snvCallingInstance.processingState == AnalysisProcessingStates.IN_PROGRESS
    }

    @Test
    void test_updateProcessingState_WhenStateIsChangedToDifferent_ShouldSucceed() {
        RoddySnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        snvCallingInstance.updateProcessingState(AnalysisProcessingStates.FINISHED)
        assert snvCallingInstance.processingState == AnalysisProcessingStates.FINISHED
    }

    @Test
    void testProcessingStateIsFailed() {
        def instance = createSnvCallingInstance()
        instance.withdrawn = true
        assert instance.validate()
    }

    private RoddySnvCallingInstance createSnvCallingInstance() {
        return DomainFactory.createRoddySnvCallingInstance([
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                samplePair        : samplePair,
        ])
    }
}
