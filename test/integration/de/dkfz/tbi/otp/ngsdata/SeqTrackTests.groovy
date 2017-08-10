package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import org.junit.Before
import org.junit.Test

class SeqTrackTests {


    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqTypeWithoutSampleType
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqTypeWithSampleType
    SeqTrack seqTrack


    @Before
    void setup() {
        seqTrack = DomainFactory.createSeqTrack().save(flush: true)
        referenceGenomeProjectSeqTypeWithSampleType = ReferenceGenomeProjectSeqType.build(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
                sampleType: seqTrack.sampleType,
        ).save(flush: true)
        referenceGenomeProjectSeqTypeWithoutSampleType = ReferenceGenomeProjectSeqType.build(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        ).save(flush: true)
    }


    @Test
    void testGetConfiguredReferenceGenome_ProjectDefault() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert referenceGenomeProjectSeqTypeWithoutSampleType.referenceGenome == referenceGenome
    }

    @Test
    void testGetConfiguredReferenceGenome_SampleTypeSpecific() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert referenceGenomeProjectSeqTypeWithSampleType.referenceGenome == referenceGenome
    }



    @Test
    void testLog() {
        seqTrack.log("Test")
        TestCase.assertContainSame(seqTrack.logMessages*.message, ["Test"])
    }

    @Test
    void testLog_Twice() {
        seqTrack.log("Test")
        seqTrack.log("Test2")
        TestCase.assertContainSame(seqTrack.logMessages*.message, ["Test", "Test2"])
    }

    @Test
    void testLog_WrongOrder() {
        seqTrack.log("Test")
        seqTrack.log("Test2")
        TestCase.assertContainSame(seqTrack.logMessages*.message, ["Test", "Test2"])
        shouldFail {
            assert seqTrack.logMessages.message[0] == "Test2"
        }
        shouldFail {
            assert seqTrack.logMessages.message[1] == "Test"
        }

    }
}
