package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

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

    Individual getIndividual() {
        return mergingSet.individual
    }

    Sample getSample() {
        return mergingSet.sample
    }

    SeqType getSeqType() {
        return mergingSet.seqType
    }

    public String toString() {
       return "id: ${mergingSet.identifier} " +
               "pass: ${identifier} " + (latestPass ? "(latest) " : "") +
               "set: ${mergingSet.identifier} " + (mergingSet.latestSet ? "(latest) " : "") +
               "<br>sample: ${mergingSet.mergingWorkPackage.sample} " +
               "seqType: ${mergingSet.mergingWorkPackage.seqType} " +
               "project: ${mergingSet.mergingWorkPackage.project}"
     }

    /**
     * @return Whether this is the most recent merging pass on the referenced {@link MergingSet}.
     */
    public boolean isLatestPass() {
        int maxIdentifier = createCriteria().get {
            eq("mergingSet", mergingSet)
            projections{
                max("identifier")
            }
        }
        return identifier == maxIdentifier
    }

    static mapping = {
        mergingSet index: "merging_pass_merging_set_idx"
    }
}
