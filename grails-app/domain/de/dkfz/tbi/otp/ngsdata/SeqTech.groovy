package de.dkfz.tbi.otp.ngsdata

class SeqTech {

    String name   // eg. solid, illumina

    static hasMany = [
        runs : Run,
        seqTracks : SeqTrack,
        seqScans : SeqScan,
        expectedFastqFiles : ExpectedSequenceFile
    ]

    static constraints = { name(unique: true) }

    String toString() {
        name
    }
}
