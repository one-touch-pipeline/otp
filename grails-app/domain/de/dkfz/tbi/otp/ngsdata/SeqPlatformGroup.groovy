package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.validation.*

/**
 * {@link SeqTrack}s from {@link SeqPlatform}s in the same {@link SeqPlatformGroup} can be merged.
 * Depending on the value of {@link MergingCriteria#seqPlatformGroup}, a SeqPlatformGroup where {@link #mergingCriteria}
 * is null or refers to the project and seqType is used
 */
class SeqPlatformGroup implements Entity, CommentableWithHistory {

    Set<SeqPlatform> seqPlatforms
    MergingCriteria mergingCriteria

    List<Comment> comments = []

    static hasMany = [
            seqPlatforms: SeqPlatform,
            comments: Comment,
    ]
    static belongsTo = SeqPlatform


    static constraints = {
        mergingCriteria nullable: true, validator: { MergingCriteria mergingCriteria1 ->
            if (mergingCriteria1) {
                return mergingCriteria1.seqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
            }
            return true
        }
        seqPlatforms validator: { Set<SeqPlatform> seqPlatforms1, SeqPlatformGroup seqPlatformGroup, Errors errors ->
            seqPlatforms1.each { SeqPlatform seqPlatform ->
                List<SeqPlatformGroup> l = withCriteria() {
                    seqPlatforms {
                        eq("id", seqPlatform.id)
                    }
                    if (seqPlatformGroup.mergingCriteria == null) {
                        isNull("mergingCriteria")
                    } else {
                        eq("mergingCriteria", seqPlatformGroup.mergingCriteria)
                    }
                    if (seqPlatformGroup.id != null) {
                        ne("id", seqPlatformGroup.id)
                    }
                }
                if (l.size() > 0) {
                    errors.rejectValue('seqPlatforms', "seqPlatform '${seqPlatform}' must not be part of multiple groups for mergingCriteria '${seqPlatformGroup.mergingCriteria}'")
                }
            }
            return
        }
    }
}
