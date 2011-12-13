package de.dkfz.tbi.otp.ngsdata

class SeqTrackBySeqScan {
    SeqTrack seqTrack
    SeqScan seqScan

    static constraints = {
        seqTrack(nullable: false)
        seqScan(nullable: false)
    }
}
