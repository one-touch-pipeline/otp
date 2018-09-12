package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.*
import org.hibernate.*

/**
 * many to many connection between
 * {@link MergingSet} and {@link ProcessedBamFile}
 */
class MergingSetAssignment implements Entity {
    static belongsTo = [
        mergingSet: MergingSet,
        bamFile: AbstractBamFile,
    ]

    static constraints = {
        bamFile validator: { AbstractBamFile bamFile, MergingSetAssignment msa ->
            if (!msa.mergingSet?.mergingWorkPackage?.satisfiesCriteria(bamFile)) {
                return false
            }
            /** Before you remove this constraint, make sure that all existing code
                correctly handles other types of {@link AbstractBamFile}s in a merging set. */
            Class bamFileClass = Hibernate.getClass(bamFile)
            return ProcessedBamFile.isAssignableFrom(bamFileClass) || ProcessedMergedBamFile.isAssignableFrom(bamFileClass)
        }
    }

    static mapping = {
        mergingSet index: "merging_set_assignment_merging_set_idx"
        bamFile index: "merging_set_assignment_bam_file_idx"
    }
}
