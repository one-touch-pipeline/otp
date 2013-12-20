package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*;

/**
 * Instance of this class represent instance of merging process
 * performed on the corresponding {@link MergingSet}.
 *
 *
 */
class MergingPass {

    /**
     * identifier unique in the scope of corresponding
     * {@link MergingSet}
     */
    int identifier

    String description

    static belongsTo = [
        mergingSet: MergingSet
    ]

    static constraints = {
        description(nullable: true)
    }

    Project getProject() {
        return mergingSet.project
    }

    public String toString() {
       return "pass:${identifier} - set:${mergingSet.identifier} - ${mergingSet.mergingWorkPackage.seqType} - ${mergingSet.mergingWorkPackage.sample}"
     }
}
