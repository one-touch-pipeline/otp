package de.dkfz.tbi.otp.ngsdata

class ExpectedSequenceFile {

    FileType fileType
    int numberOfRepeats = 1       // default value

    static belongsTo = [
        seqTech : SeqTech,
        seqType : SeqType
    ]

    static constraints = {
        fileType(nullable: false)
        seqTech()
        seqType()
    }
}
