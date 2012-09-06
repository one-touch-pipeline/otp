package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class AlignmentPass {

    int identifier
    SeqTrack seqTrack
    String description

    static belongsTo = [
        seqTrack: SeqTrack
    ]

    static constraints = {
        // seqTrack and identifier are unique
        description(nullable: true)
    }

    public String getDirectory() {
        return "pass${identifier}"
    }

    public String toString() {
        return "pass:${identifier} on run:${seqTrack.run.name} lane:${seqTrack.laneId}"
    }
}
