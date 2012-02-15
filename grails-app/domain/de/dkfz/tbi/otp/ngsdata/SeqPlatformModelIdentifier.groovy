package de.dkfz.tbi.otp.ngsdata

class SeqPlatformModelIdentifier {

    String name
    static belongsTo = [seqPlatform: SeqPlatform]
    static constraints = {
        name(nullable: false)
    }
}
