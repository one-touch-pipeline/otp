package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*


@TestMixin(GrailsUnitTestMixin)
@TestFor(SeqTrack)
@Mock([DataFile])
class SeqTrackUnitTests {

    void testIsWithDrawn() {
        SeqTrack seqTrack = new SeqTrack()
        assert null != seqTrack.save(validate: false)
        assertFalse seqTrack.withdrawn

        DataFile dataFile = new DataFile(seqTrack: seqTrack, fileWithdrawn: false)
        assert null != dataFile.save(validate: false)
        assertFalse seqTrack.withdrawn

        dataFile = new DataFile(seqTrack: seqTrack, fileWithdrawn: false)
        assert null != dataFile.save(validate: false)
        assertFalse seqTrack.withdrawn

        dataFile.fileWithdrawn = true
        assert null != dataFile.save(validate: false)
        assertTrue seqTrack.withdrawn
    }
}
