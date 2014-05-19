package de.dkfz.tbi.otp.ngsdata

class ExpectedSequenceFile {

    FileType fileType
    int numberOfRepeats = 1       // default value

    static belongsTo = [
        seqTech : SeqPlatform,
        seqType : SeqType
    ]

    static constraints = {
        fileType(nullable: false)
        seqTech()
        seqType()
    }

    static mapping = {
        seqTech index: "expected_sequence_file_seq_tech_idx"
        seqType index: "expected_sequence_file_seq_type_idx"
    }
}
