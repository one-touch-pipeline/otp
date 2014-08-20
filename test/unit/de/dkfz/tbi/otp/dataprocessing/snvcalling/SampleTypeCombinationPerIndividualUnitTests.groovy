package de.dkfz.tbi.otp.dataprocessing.snvcalling

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(SampleTypeCombinationPerIndividual)
@Mock([Individual, SampleType, SeqType])
class SampleTypeCombinationPerIndividualUnitTests {

    void testSaveSnvCombinationPerIndividualOnlyIndividual() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.individual = new Individual()
        assertFalse(sampleCombinationPerIndividual.validate())
    }

    void testSaveSnvCombinationPerIndividualOnlySampleType1() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.sampleType1 = new SampleType()
        assertFalse(sampleCombinationPerIndividual.validate())
    }

    void testSaveSnvCombinationPerIndividualOnlySampleType2() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.sampleType2 = new SampleType()
        assertFalse(sampleCombinationPerIndividual.validate())
    }

    void testSaveSnvCombinationPerIndividualOnlySeqType() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.seqType = new SeqType()
        assertFalse(sampleCombinationPerIndividual.validate())
    }

    void testSaveSnvCombinationPerIndividual() {
        SampleTypeCombinationPerIndividual sampleCombinationPerIndividual = new SampleTypeCombinationPerIndividual()
        sampleCombinationPerIndividual.individual = new Individual()
        sampleCombinationPerIndividual.seqType = new SeqType()
        sampleCombinationPerIndividual.sampleType1 = new SampleType()
        sampleCombinationPerIndividual.sampleType2 = new SampleType()
        assertTrue(sampleCombinationPerIndividual.validate())
    }
}
