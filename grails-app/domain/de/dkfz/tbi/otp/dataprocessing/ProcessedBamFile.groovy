package de.dkfz.tbi.otp.dataprocessing

class ProcessedBamFile extends AbstractFileSystemBamFile {

    /**
     * this flag is used to block the object for the time
     * it is being used to create a new {@link MergingSet}.
     * After the new {@link MergingSet} has been created,
     * the object must released from block.
     * If true, the object is being used to create a new
     * {@link MergingSet}.
     */
    boolean isBlockedForNewMergingSet = false

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]
}
