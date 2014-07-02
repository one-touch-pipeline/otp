package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(FastqSet)
@Mock([SeqTrack, Sample, SeqType])
class FastqSetUnitTests {

    void testConstraints() {
        Sample sample = new Sample()
        SeqType seqType1 = new SeqType()
        SeqType seqType2 = new SeqType()

        SeqTrack seqTrack1 = new SeqTrack(
                sample: sample,
                seqType: seqType1
        )
        seqTrack1.save(flush: true)

        SeqTrack seqTrack2 = new SeqTrack(
                sample: sample,
                seqType: seqType2
        )
        seqTrack2.save(flush: true)

        SeqTrack seqTrack3 = new SeqTrack(
                sample: sample,
                seqType: seqType1
        )
        seqTrack3.save(flush: true)

        FastqSet fastqSet = new FastqSet(
                seqTracks: new HashSet<SeqTrack>([seqTrack1, seqTrack2])
        )
        assertFalse(fastqSet.validate())

        FastqSet fastqSet2 = new FastqSet(
                seqTracks: new HashSet<SeqTrack>([seqTrack1, seqTrack3])
        )
        assertTrue(fastqSet2.validate())
    }
}
