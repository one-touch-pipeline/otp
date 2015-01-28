package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome

class SeqTrackTests extends TestCase {



    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqTypeWithoutSampleType
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqTypeWithSampleType
    SeqTrack seqTrack



    @Before
    void setup() {
        seqTrack = SeqTrack.build()
        referenceGenomeProjectSeqTypeWithSampleType = ReferenceGenomeProjectSeqType.build(
            project: seqTrack.project,
            seqType: seqTrack.seqType,
            sampleType: seqTrack.sampleType,
            )
        referenceGenomeProjectSeqTypeWithoutSampleType = ReferenceGenomeProjectSeqType.build(
            project: seqTrack.project,
            seqType: seqTrack.seqType,
            )
    }



    @Test
    void testGetConfiguredReferenceGenome_ProjectDefault() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert referenceGenomeProjectSeqTypeWithoutSampleType.referenceGenome == referenceGenome
    }

    @Test
    void testGetConfiguredReferenceGenome_ProjectDefault_WrongProject() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        referenceGenomeProjectSeqTypeWithoutSampleType.project = Project.build()

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert null == referenceGenome
    }

    @Test
    void testGetConfiguredReferenceGenome_ProjectDefault_WrongSeqType() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        referenceGenomeProjectSeqTypeWithoutSampleType.seqType = SeqType.build()

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert null == referenceGenome
    }



    @Test
    void testGetConfiguredReferenceGenome_SampleTypeSpecific() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert referenceGenomeProjectSeqTypeWithSampleType.referenceGenome == referenceGenome
    }

    @Test
    void testGetConfiguredReferenceGenome_SampleTypeSpecific_WrongProject() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        referenceGenomeProjectSeqTypeWithSampleType.project = Project.build()

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert null == referenceGenome
    }

    @Test
    void testGetConfiguredReferenceGenome_SampleTypeSpecific_WrongSeqType() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        referenceGenomeProjectSeqTypeWithSampleType.seqType = SeqType.build()

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert null == referenceGenome
    }

    @Test
    void testGetConfiguredReferenceGenome_SampleTypeSpecific_WrongSampleType() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        referenceGenomeProjectSeqTypeWithSampleType.sampleType = SampleType.build()

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert null == referenceGenome
    }



    @Test
    void testGetConfiguredReferenceGenome_NotDefined() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.UNKNOWN

        String message = shouldFail(RuntimeException) {
            seqTrack.getConfiguredReferenceGenome()
        }
        assert message.contains(SampleType.SpecificReferenceGenome.UNKNOWN.toString())
    }

}
