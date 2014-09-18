package de.dkfz.tbi.otp.ngsdata

class SampleType {

    String name
    static constraints = {
        name(unique: true)
    }

    String getDirName() {
        return name.toLowerCase()
    }
}
