package de.dkfz.tbi.otp.ngsdata

class SampleType {

    String name
    static constraints = {
        name(unique: true)
        // TODO: OTP-1122: unique constraint for dirName
    }

    String getDirName() {
        return name.toLowerCase()
    }
}
