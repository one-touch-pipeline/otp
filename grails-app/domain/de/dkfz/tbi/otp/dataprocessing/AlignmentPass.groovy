package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*;

class AlignmentPass {

    int identifier
    SeqTrack seqTrack
    String description
    State status = State.PROCESSING

    // TODO: if the wf has failed -> seqTrack is set to NOT_STARTED manually
    // at the same time state of this object must be set to FAILED
    enum State {PROCESSING, SUCCEED, FAILED}

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

    // TODO: udpate/create toString methods for all the new/updated dom
    public String toString() {
        return "pass:${identifier} on run:${seqTrack.run.name} lane:${seqTrack.laneId}"
    }
}
