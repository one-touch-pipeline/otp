package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

class SeqCenter implements Entity {

    String name
    String dirName
    boolean autoImportable = false

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false, unique: true, validator: { OtpPath.isValidPathComponent(it) })
    }

    String toString() {
        name
    }
}
