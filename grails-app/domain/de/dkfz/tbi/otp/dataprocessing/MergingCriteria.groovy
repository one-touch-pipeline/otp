package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class MergingCriteria implements Entity {

    Project project
    SeqType seqType
    boolean libPrepKit = true
    boolean seqPlatformGroup = true

    static constraints = {
        libPrepKit validator : { val, obj ->
            if (obj.seqType.isExome() && !val) {
                return "In case of Exome data, the libraryPreparationKit must be part of the MergingCriteria"
            }
        }
    }
}
