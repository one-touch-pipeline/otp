package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.validation.*

/**
 * {@link SeqTrack}s from {@link SeqPlatform}s in the same {@link SeqPlatformGroup} can be merged.
 * Depending on the value of {@link MergingCriteria#useSeqPlatformGroup}, a SeqPlatformGroup where {@link #mergingCriteria}
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
                return mergingCriteria1.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
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

    @Override
    String toString() {
        StringBuilder sb = new StringBuilder()
        if (mergingCriteria) {
            sb << "'${mergingCriteria.project} ${mergingCriteria.seqType?.displayNameWithLibraryLayout}'"
        } else {
            sb << "'OTP global'"
        }
        sb << " seq platform group with "
        if (seqPlatforms) {
            sb << seqPlatforms*.toString().sort().collect {
                "'${it}'"
            }.join(", ")
        } else {
            sb << " no seq platforms"
        }
        return sb.toString()
    }
}
