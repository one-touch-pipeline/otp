package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class ProjectSeqType implements Entity {

    Project project
    SeqType seqType


    SpecificSeqPlatformGroups useSeqPlatformGroups = SpecificSeqPlatformGroups.USE_OTP_DEFAULT

    enum SpecificSeqPlatformGroups {
        USE_OTP_DEFAULT,
        USE_PROJECT_SEQ_TYPE_SPECIFIC,
    }

    static constraints = {
        project unique: 'seqType'
    }
}
