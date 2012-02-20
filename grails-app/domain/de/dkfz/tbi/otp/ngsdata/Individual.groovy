package de.dkfz.tbi.otp.ngsdata

class Individual {

    String pid                 // real pid from iChip
    String mockPid             // pid used in the project
    String mockFullName        // mnemonic used in the project

    enum Type {REAL, POOL, CELLLINE, UNDEFINED}
    Type type

    static belongsTo = [project : Project]

    static constraints = {
        pid(unique: true, nullable: false)
    }

    String toString() {
        "${mockPid} ${mockFullName}"
    }
}
