package de.dkfz.tbi.otp.ngsdata

class Project {

    String name
    String dirName
    String realmName

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false)
        realmName(blank: false)
    }

    String toString() {
        name
    }
}
