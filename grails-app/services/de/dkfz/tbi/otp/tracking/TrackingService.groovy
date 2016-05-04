package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class TrackingService {

    public OtrsTicket createOtrsTicket(String ticketNumber) {
        OtrsTicket otrsTicket = new OtrsTicket(ticketNumber: ticketNumber)
        assert otrsTicket.save(flush: true, failOnError: true)
        return otrsTicket
    }

    public OtrsTicket createOrResetOtrsTicket(String ticketNumber) {
        OtrsTicket otrsTicket = CollectionUtils.atMostOneElement(OtrsTicket.findAllByTicketNumber(ticketNumber))
        if (!otrsTicket) {
            return createOtrsTicket(ticketNumber)
        } else {
            otrsTicket.installationFinished = null
            otrsTicket.fastqcFinished = null
            otrsTicket.alignmentFinished = null
            otrsTicket.snvFinished = null
            otrsTicket.finalNotificationSent = false
            assert otrsTicket.save(flush: true, failOnError: true)
            return otrsTicket
        }
    }

    public void setStartedForSeqTracks(Collection<SeqTrack> seqTracks, OtrsTicket.ProcessingStep step) {
        setStarted(findAllOtrsTickets(seqTracks), step)
    }

    public Set<OtrsTicket> findAllOtrsTickets(Collection<SeqTrack> seqTracks) {
        Set<OtrsTicket> otrsTickets = [] as Set
        //Doesn't work as a single Query, probably a Unit test problem
        DataFile.withCriteria {
            'in' ('seqTrack', seqTracks)
            projections {
                distinct('runSegment')
            }
        }.each {
            if (it?.otrsTicket) {
                otrsTickets.add(it.otrsTicket)
            }
        }
        return otrsTickets
    }

    public void setStarted(Collection<OtrsTicket> otrsTickets, OtrsTicket.ProcessingStep step) {
        otrsTickets.unique().each {
            if (it."${step}Started" == null) {
                it."${step}Started" = new Date()
                assert it.save(flush: true)
            }
        }
    }
}
