package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.*


class ToolName implements Entity {

    String name

    static constraints = {
        name unique: true
    }
}
