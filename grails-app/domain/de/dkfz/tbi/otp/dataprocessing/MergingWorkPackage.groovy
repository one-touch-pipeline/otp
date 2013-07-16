package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*


/**
 * Represents a "workpackage" to merge all {@link ProcessedBamFile}s of
 * the corresponding {@link SeqType}, which are available for the corresponding
 * {@link Sample} (currently and in the future) and satisfying the given
 * {@link MergingCriteria} or custom selection.
 *
 *
 */
class MergingWorkPackage {

    /**
     * Identifies the way how this {@link MergingWorkPackage} is maintained (updated):
     */
    enum ProcessingType {
        /**
         * Files to be merged are defined by the user according to his own rules
         * (but still criteria can be used).
         * The new {@link MergingSet} are added to the {@link MergingWorkPackage} manually.
         * The user defines when to start processing of certain {@link MergingSet} from
         * this {@link MergingWorkPackage}
         */
        MANUAL,
        /**
         * The {@link MergingWorkPackage} is maintained by the system.
         * New {@link MergingSet}s are added by the system based on the presence of
         * new {@link ProcessedBamFile}s and {@link MergingCriteria}.
         * Processing of {@link MergingSet} is started automatically.
         */
        SYSTEM
    }

    /**
     * a criteria to join {@link ProcessedBamFile}s of one sample into {@link MergingSet}s
     */
    enum MergingCriteria {
        /**
         * {@link ProcessedBamFile}s for this {@link Sample} must be generated
         * from sequencing files with the same {@link SeqPlatform} and {@link SeqType}
         */
        DEFAULT
        // e.g. ILLUMINA_2000_2500: illumina 2000 and 2500 and the same seq type
    }

    /**
     * {@link ProcessingType} of this instance of {@link MergingWorkPackage}
     */
    ProcessingType processingType = ProcessingType.SYSTEM

    /**
     * {@link MergingCriteria} used to create {@link MergingSet}s
     * in this instance of {@link MergingWorkPackage}
     */
    MergingCriteria mergingCriteria = MergingCriteria.DEFAULT

    static belongsTo = [
        sample: Sample,
        seqType: SeqType]

    static constraints = {
        mergingCriteria (nullable: true)
    }
}
