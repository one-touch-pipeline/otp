package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import grails.buildtestdata.mixin.Build

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(FastqSet)
@Build([Sample, SeqTrack, SeqType])
class FastqSetUnitTests {

    void testConstraints() {
        Sample sample = Sample.build()
        SeqType seqType1 = SeqType.build()
        SeqType seqType2 = SeqType.build()

        SeqTrack seqTrack1 = SeqTrack.build(
                sample: sample,
                seqType: seqType1
        )
        seqTrack1.save(flush: true)

        SeqTrack seqTrack2 = SeqTrack.build(
                sample: sample,
                seqType: seqType2
        )
        seqTrack2.save(flush: true)

        SeqTrack seqTrack3 = SeqTrack.build(
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
