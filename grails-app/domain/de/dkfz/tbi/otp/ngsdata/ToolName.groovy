package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*


class ToolName implements Entity {

    enum Type {
        RNA,
        SINGLE_CELL,
    }

    String name
    Type type
    String path

    static constraints = {
        name unique: true
        type nullable: false
        path nullable: false, blank: false, unique: true, validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
    }

    static hasMany = [
            referenceGenomeIndexes: ReferenceGenomeIndex,
    ]
}
