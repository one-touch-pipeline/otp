package de.dkfz.tbi.otp.ngsdata

class SeqTech {

    String name   // eg. solid, illumina

    static constraints = { name(unique: true) }

    String toString() {
        name
    }
}
