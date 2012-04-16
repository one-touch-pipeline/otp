package de.dkfz.tbi.otp.ngsdata

class Project {

    String name
    String dirName

    static belongsTo = [realm: Realm]

    static constraints = {
        name(blank: false, unique: true)
        dirName(blank: false)
    }

    String toString() {
        name
    }
}
