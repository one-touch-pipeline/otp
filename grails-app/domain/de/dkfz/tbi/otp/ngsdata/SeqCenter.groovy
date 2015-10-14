package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath

class SeqCenter {

    String name
    String dirName

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false, unique: true, validator: { OtpPath.isValidPathComponent(it) })
    }

    String toString() {
        name
    }
}
