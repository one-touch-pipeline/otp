package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.validation.*
import org.junit.*

class SamplePairTests {

    @After
    void tearDown() {
        SamplePair.metaClass = null
    }

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
        MergingWorkPackage mwp1 = DomainFactory.createMergingWorkPackage()
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
        assert persistedSamplePair.save(flush: true)

        SamplePair.setSnvProcessingStatus([nonPersistedSamplePair, persistedSamplePair], processingStatus)

        assert nonPersistedSamplePair.snvProcessingStatus == processingStatus
        assert nonPersistedSamplePair.id
        assert persistedSamplePair.snvProcessingStatus == processingStatus
    }

    @Test
    void testConstraints_DifferentIndividual_shouldFail() {
        SamplePair samplePair = DomainFactory.createSamplePair()

        MergingWorkPackage mergingWorkPackage1 = samplePair.mergingWorkPackage1
        mergingWorkPackage1.sample.individual = DomainFactory.createIndividual()
        assert mergingWorkPackage1.sample.save(flush: true)

        SamplePair.metaClass.getIndividual = { -> return mergingWorkPackage1.individual}

        TestCase.shouldFailWithMessageContaining(ValidationException, "individual", { samplePair.save() })
    }

    @Test
    void testConstraints_DifferentSeqType_ShouldFail() {
        SamplePair samplePair = DomainFactory.createSamplePair()
        MergingWorkPackage mergingWorkPackage1 = samplePair.mergingWorkPackage1
        mergingWorkPackage1.seqType = DomainFactory.createSeqType()
        assert mergingWorkPackage1.save(flush: true)

        SamplePair.metaClass.getSeqType = { -> return mergingWorkPackage1.seqType }

        TestCase.shouldFailWithMessageContaining(ValidationException, "seqType", { samplePair.save(flush: true) })
    }

    @Test
    void testFindLatestSnvCallingInstance_whenNoSnvCallingInstanceExists_ShouldReturnNull() {
        SamplePair sp = DomainFactory.createSamplePair()

        assert null == sp.findLatestSnvCallingInstance()
    }

    @Test
    void testFindLatestSnvCallingInstance_whenSnvCallingInstanceExists_ShouldReturnLatest() {
        SnvCallingInstance first = DomainFactory.createSnvInstanceWithRoddyBamFiles()
        SnvCallingInstance latest = DomainFactory.createSnvInstanceWithRoddyBamFiles([samplePair: first.samplePair, instanceName: '2015-08-25_15h32'])

        assert latest == latest.samplePair.findLatestSnvCallingInstance()
    }

    @Test
    void testFindLatestIndelCallingInstance_whenNoIndelCallingInstanceExists_ShouldReturnNull() {
        SamplePair sp = DomainFactory.createSamplePair()

        assert null == sp.findLatestIndelCallingInstance()
    }

    @Test
    void testFindLatestIndelCallingInstance_whenIndelCallingInstanceExists_ShouldReturnLatest() {
        IndelCallingInstance first = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles([instanceName: 'instance1'])
        IndelCallingInstance latest = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles([samplePair: first.samplePair, instanceName: 'instance2'])

        assert latest == latest.samplePair.findLatestIndelCallingInstance()
    }

    @Test
    void testFindLatestSophiaInstance_whenSophiaInstanceExists_ShouldReturnNull() {
        SamplePair sp = DomainFactory.createSamplePair()

        assert null == sp.findLatestSophiaInstance()
    }

    @Test
    void testFindLatestSophiaCallingInstance_whenSophiaInstancesExists_ShouldReturnLatest() {
        SophiaInstance first = DomainFactory.createSophiaInstanceWithRoddyBamFiles([instanceName: 'instance1'])
        SophiaInstance latest = DomainFactory.createSophiaInstanceWithRoddyBamFiles([samplePair: first.samplePair, instanceName: 'instance2'])

        assert latest == latest.samplePair.findLatestSophiaInstance()
    }

}
