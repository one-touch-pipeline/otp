package de.dkfz.tbi.otp.ngsdata

class Project {

    String name
    String dirName
    String host      // es. BioQuant, DKFZ

    static hasMany = [
        runs : Run,
        individuals: Individual,
    ]

    static constraints = {

        name(blank: false, unique: true)
        dirName(blank: false)
        host(blank: false)
    }

    String toString() {
        name
    }
}
