package de.dkfz.tbi.otp.ngsdata

class Sample {

    enum Type {
        TUMOR, CONTROL, UNKNOWN
    }
    Type type
    String subType             // hedge for the future, eg. tumor 1, tumor 2

    static belongsTo = [individual : Individual]

    static constraints = {
        type()
        subType(nullable: true)
    }

    String toString() {
        // usefulll for scaffolding
        "${individual.mockFullName} ${type}"
    }
}
