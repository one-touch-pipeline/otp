package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.ToString

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.Entity

@ToString(includeNames=true)
class MergingCriteria implements Entity {

    Project project
    SeqType seqType
    boolean useLibPrepKit = true
    SpecificSeqPlatformGroups useSeqPlatformGroup = SpecificSeqPlatformGroups.USE_OTP_DEFAULT

    enum SpecificSeqPlatformGroups {
        USE_OTP_DEFAULT,
        USE_PROJECT_SEQ_TYPE_SPECIFIC,
        IGNORE_FOR_MERGING,
    }

    static constraints = {
        useLibPrepKit validator : { val, obj ->
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
