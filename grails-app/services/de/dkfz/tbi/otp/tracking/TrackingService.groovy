package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.ngsdata.*

class TrackingService {

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
