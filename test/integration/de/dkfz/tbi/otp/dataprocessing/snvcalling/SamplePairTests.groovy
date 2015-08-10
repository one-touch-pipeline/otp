package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class SamplePairTests extends GroovyTestCase {

    @Test
    void testSetProcessingStatusNeedsProcessing() {
        testSetNeedsProcessing(ProcessingStatus.NEEDS_PROCESSING)
    }

    @Test
    void testSetProcessingStatusNoProcessingNeeded() {
        testSetNeedsProcessing(ProcessingStatus.NO_PROCESSING_NEEDED)
    }

    @Test
    void testSetProcessingStatusDisabled() {
        testSetNeedsProcessing(ProcessingStatus.DISABLED)
    }

    private void testSetNeedsProcessing(final ProcessingStatus processingStatus) {
        MergingWorkPackage mwp1 = MergingWorkPackage.build()
        SampleTypePerProject.build(project: mwp1.project, sampleType: mwp1.sampleType, category: SampleType.Category.DISEASE)
        final SamplePair nonPersistedSamplePair = SamplePair.createInstance(
                mergingWorkPackage1: mwp1,
                mergingWorkPackage2: DomainFactory.createMergingWorkPackage(mwp1),
                processingStatus: processingStatus,  // Tests that the instance is persisted even if it already has the correct value.
        )
        final SamplePair persistedSamplePair = SamplePair.createInstance(
                mergingWorkPackage1: mwp1,
                mergingWorkPackage2: DomainFactory.createMergingWorkPackage(mwp1),
                processingStatus: processingStatus == ProcessingStatus.NEEDS_PROCESSING ? ProcessingStatus.NO_PROCESSING_NEEDED : ProcessingStatus.NEEDS_PROCESSING,
        )
        assert persistedSamplePair.save()

        SamplePair.setProcessingStatus([nonPersistedSamplePair, persistedSamplePair], processingStatus)

        assert nonPersistedSamplePair.processingStatus == processingStatus
        assert nonPersistedSamplePair.id
        assert persistedSamplePair.processingStatus == processingStatus
    }
}
