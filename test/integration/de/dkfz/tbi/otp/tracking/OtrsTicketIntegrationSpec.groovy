package de.dkfz.tbi.otp.tracking

import grails.test.spock.IntegrationSpec

import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.TestCase.assertContainSame

class OtrsTicketIntegrationSpec extends IntegrationSpec {

    void 'findAllSeqTracks finds expected SeqTracks'() {
        given:
        OtrsTicket ticketA = DomainFactory.createOtrsTicket()
        RunSegment runSegmentA1 = DomainFactory.createRunSegment(otrsTicket: ticketA)
        RunSegment runSegmentA2 = DomainFactory.createRunSegment(otrsTicket: ticketA)
        SeqTrack seqTrackA1 = DomainFactory.createSeqTrackWithOneDataFile([:], [runSegment: runSegmentA1])
        SeqTrack seqTrackA2 = DomainFactory.createSeqTrackWithOneDataFile([:], [runSegment: runSegmentA2])
        SeqTrack seqTrackA3 = DomainFactory.createSeqTrackWithOneDataFile([:], [runSegment: runSegmentA2])

        OtrsTicket ticketB = DomainFactory.createOtrsTicket()
        RunSegment runSegmentB1 = DomainFactory.createRunSegment(otrsTicket: ticketB)
        SeqTrack seqTrackB1 = DomainFactory.createSeqTrackWithTwoDataFiles([:], [runSegment: runSegmentB1], [:])

        OtrsTicket ticketC = DomainFactory.createOtrsTicket()

        expect:
        assertContainSame(ticketA.findAllSeqTracks(), [seqTrackA1, seqTrackA2, seqTrackA3])
        assertContainSame(ticketB.findAllSeqTracks(), [seqTrackB1])
        ticketC.findAllSeqTracks().isEmpty()
    }
}
