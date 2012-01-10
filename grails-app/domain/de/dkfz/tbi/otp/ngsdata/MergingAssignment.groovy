package de.dkfz.tbi.otp.ngsdata

class MergingAssignment {
    SeqTrack seqTrack
    SeqScan seqScan

    static constraints = {
        seqTrack(nullable: false)
        seqScan(nullable: false)
    }
}
