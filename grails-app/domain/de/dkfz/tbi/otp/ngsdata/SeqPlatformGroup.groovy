package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.validation.*

/**
 * {@link SeqTrack}s from {@link SeqPlatform}s in the same {@link SeqPlatformGroup} can be merged.
 * Depending on the value of {@link ProjectSeqType#useSeqPlatformGroups}, a SeqPlatformGroup where {@link #projectSeqType}
 * is null or refers to the project and seqType is used
 */
class SeqPlatformGroup implements Entity {

    Set<SeqPlatform> seqPlatforms
    ProjectSeqType projectSeqType

    Comment comment

    static hasMany = [
            seqPlatforms: SeqPlatform,
    ]
    static belongsTo = SeqPlatform


    static constraints = {
        projectSeqType nullable: true
        comment nullable: true
        seqPlatforms validator: { Set<SeqPlatform> seqPlatforms1, SeqPlatformGroup seqPlatformGroup, Errors errors ->
            seqPlatforms1.each { SeqPlatform seqPlatform ->
                List<SeqPlatformGroup> l = withCriteria() {
                    seqPlatforms {
                        eq("id", seqPlatform.id)
                    }
                    if (seqPlatformGroup.projectSeqType == null) {
                        isNull("projectSeqType")
                    } else {
                        eq("projectSeqType", seqPlatformGroup.projectSeqType)
                    }
                    ne("id", seqPlatformGroup.id)
                }
                if (l.size() > 0) {
                    errors.rejectValue('seqPlatforms', "seqPlatform '${seqPlatform}' must not be part of muliple groups for project '${seqPlatformGroup.projectSeqType}'")
                }
            }
            return
        }
    }
}
