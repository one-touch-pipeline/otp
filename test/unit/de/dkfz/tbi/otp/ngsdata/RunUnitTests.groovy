package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Test

import static org.junit.Assert.*

@TestFor(Run)
@Build([
    SeqPlatformGroup,
    SeqTrack,
])
class RunUnitTests {

    @Test
    void testGetSeqType() {

        Run run = Run.build()

        SeqType seqType1 = SeqType.build()
        SeqType seqType2 = SeqType.build()

        DomainFactory.createSeqTrack(
            run: run,
            seqType: seqType1,
        )

        DomainFactory.createSeqTrack(
            run: run,
            seqType: seqType1,
        )

        assertNotNull(run.getSeqType())

        DomainFactory.createSeqTrack(
             run: run,
             seqType: seqType2,
        )

        assertNull(run.getSeqType())
    }

    @Test
    void testGetIndividual() {

        Run run = Run.build()

        Individual individual1 = Individual.build()
        Individual individual2 = Individual.build()

        Sample sample1 = Sample.build(
            individual: individual1
        )

        Sample sample2 = Sample.build(
            individual: individual2
        )

        DomainFactory.createSeqTrack(
            run: run,
            sample: sample1
        )

        DomainFactory.createSeqTrack(
            run: run,
            sample: sample1
        )

        assertEquals(individual1, run.getIndividual())

        SeqTrack.build (
            run: run,
            sample: sample2
        )

        assertNull(run.getIndividual())
    }
}
