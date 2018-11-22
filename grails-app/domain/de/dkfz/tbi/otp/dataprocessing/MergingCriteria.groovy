package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import groovy.transform.ToString

@ToString(includeNames=true)
class MergingCriteria implements Entity {

    Project project
    SeqType seqType
    boolean libPrepKit = true //rename
    SpecificSeqPlatformGroups seqPlatformGroup = SpecificSeqPlatformGroups.USE_OTP_DEFAULT //rename

    enum SpecificSeqPlatformGroups {
        USE_OTP_DEFAULT,
        USE_PROJECT_SEQ_TYPE_SPECIFIC,
        IGNORE_FOR_MERGING,
    }

    static constraints = {
        libPrepKit validator : { val, obj ->
            if (obj.seqType.isExome() && !val) {
                return "In case of Exome data, the libraryPreparationKit must be part of the MergingCriteria"
            }
            if (obj.seqType.isWgbs() && val) {
                return "In case of WGBS data, the libraryPreparationKit must not be part of the MergingCriteria"
            }
        }
        project unique: 'seqType'
    }
}
