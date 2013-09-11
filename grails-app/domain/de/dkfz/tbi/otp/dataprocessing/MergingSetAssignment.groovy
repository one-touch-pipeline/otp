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
}