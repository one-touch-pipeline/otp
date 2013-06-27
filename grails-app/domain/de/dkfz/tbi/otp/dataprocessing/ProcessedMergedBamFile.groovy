package de.dkfz.tbi.otp.dataprocessing

/**
 * Represents a merged bam file stored on the file system
 * and produced by the merging process identified by the
 * given {@link MergingPass}
 *
 *
 */
class ProcessedMergedBamFile extends AbstractFileSystemBamFile {

    static belongsTo = [
        mergingPass: MergingPass
    ]
}
