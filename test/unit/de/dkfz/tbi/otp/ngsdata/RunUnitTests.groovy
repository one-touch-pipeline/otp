package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.buildtestdata.mixin.Build
import org.junit.*

@TestFor(Run)
@Build([
    SeqTrack,
])
class RunUnitTests {

    void testGetSeqType() {

        Run run = Run.build()

        SeqType seqType1 = SeqType.build()
        SeqType seqType2 = SeqType.build()

        SeqTrack.build(
            run: run,
            seqType: seqType1,
        )

        SeqTrack.build(
            run: run,
            seqType: seqType1,
        )

        assertNotNull(run.getSeqType())

        SeqTrack.build(
             run: run,
             seqType: seqType2,
        )

        assertNull(run.getSeqType())
    }
}
