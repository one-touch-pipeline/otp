package de.dkfz.tbi.otp.ngsdata

class SeqPlatformModelIdentifier {

    String name
    static belongsTo = [seqPlatform: SeqPlatform]
    static constraints = {
        name(nullable: false)
    }

    static mapping = {
        seqPlatform index: "seq_platform_model_identifier_seq_platform_idx"
    }
}
