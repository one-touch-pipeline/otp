package de.dkfz.tbi.otp.dataprocessing

/**
 * many to many connection between
 * {@link MergingSet} and {@link ProcessedBamFile}
 *
 *
 */
class MergingSetAssignment {
    static belongsTo = [
        mergingSet: MergingSet,
        bamFile: AbstractBamFile
    ]

    static mapping = {
        mergingSet index: "merging_set_assignment_merging_set_idx"
        bamFile index: "merging_set_assignment_bam_file_idx"
    }
}
