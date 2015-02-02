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

    static constraints = {
        bamFile validator: { AbstractBamFile bamFile, MergingSetAssignment msa ->
            if (!msa.mergingSet?.mergingWorkPackage?.satisfiesMergingCriteria(bamFile)) {
                return false
            }
            /** Before you remove this constraint, make sure that all existing code
                correctly handles other types of {@link AbstractBamFile}s in a merging set. */
            return bamFile instanceof ProcessedBamFile || bamFile instanceof ProcessedMergedBamFile
        }
    }

    static mapping = {
        mergingSet index: "merging_set_assignment_merging_set_idx"
        bamFile index: "merging_set_assignment_bam_file_idx"
    }
}
