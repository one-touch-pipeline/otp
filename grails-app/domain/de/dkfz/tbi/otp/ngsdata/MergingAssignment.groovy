package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class MergingAssignment implements Entity {
    SeqTrack seqTrack
    SeqScan seqScan

    static constraints = {
        seqTrack(nullable: false)
        seqScan(nullable: false)
    }
}
