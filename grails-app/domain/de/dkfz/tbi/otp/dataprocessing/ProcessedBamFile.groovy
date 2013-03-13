package de.dkfz.tbi.otp.dataprocessing

class ProcessedBamFile extends AbstractFileSystemBamFile {

    enum MergingBlock {
        NOT_STARTED,
        IN_PROGRESS
    }

    MergingBlock mergingBlock = MergingBlock.NOT_STARTED

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]
}
