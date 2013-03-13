package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*;

class MergingSet {

    int identifier

    enum State {
        DECLARED, NEEDS_PROCESSING, INPROGRESS, PROCESSED
    }

    State status = State.DECLARED

    static belongsTo = [
        mergingWorkPackage: MergingWorkPackage
    ]
}
