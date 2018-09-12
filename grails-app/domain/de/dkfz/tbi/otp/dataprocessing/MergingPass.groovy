package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity

/**
 * Instance of this class represent instance of merging process
 * performed on the corresponding {@link MergingSet}.
 */
class MergingPass implements ProcessParameterObject, Entity {

    /**
     * identifier unique in the scope of corresponding
     * {@link MergingSet}
     */
    int identifier

    String description

    static belongsTo = [
        mergingSet: MergingSet,
    ]

    static constraints = {
        identifier(unique: 'mergingSet')
        description(nullable: true)
    }

    Project getProject() {
        return mergingSet.project
    }

    @Override
    short getProcessingPriority() {
        return project.processingPriority
    }

    @Override
    Individual getIndividual() {
        return mergingSet.individual
    }

    Sample getSample() {
        return mergingSet.sample
    }

    SampleType getSampleType() {
        return mergingSet.sampleType
    }

    @Override
    SeqType getSeqType() {
        return mergingSet.seqType
    }

    MergingWorkPackage getMergingWorkPackage() {
        return mergingSet.mergingWorkPackage
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return mergingSet.containedSeqTracks
    }

    public String toString() {
        return "id: ${mergingSet.id} " +
        "pass: ${identifier} " + (latestPass ? "(latest) " : "") +
        "set: ${mergingSet.identifier} " + (mergingSet.latestSet ? "(latest) " : "") +
        "<br>sample: ${sample} " +
        "seqType: ${seqType} " +
        "<br>project: ${project}"
    }

    /**
     * @return Whether this is the most recent merging pass on the referenced {@link MergingSet}.
     */
    public boolean isLatestPass() {
        return identifier == maxIdentifier(mergingSet)
    }

    public static Integer maxIdentifier(final MergingSet mergingSet) {
        assert mergingSet
        return MergingPass.createCriteria().get {
            eq("mergingSet", mergingSet)
            projections {
                max("identifier")
            }
        }
    }

    public static int nextIdentifier(final MergingSet mergingSet) {
        assert mergingSet
        final Integer maxIdentifier = maxIdentifier(mergingSet)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

    static mapping = { mergingSet index: "merging_pass_merging_set_idx" }
}
