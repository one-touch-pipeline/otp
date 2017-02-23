package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*


class ToolName implements Entity {

    String name
    String path

    static constraints = {
        name unique: true
        path nullable: false, blank: false, unique: true, validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
    }
}
