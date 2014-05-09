package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Represents a set of {@link ProcessedBamFile}s to be merged
 * for one {@link Sample}. The files are selected using the criteria
 * defined in the corresponding {@link MergingWorkPackage}.
 * A {@link MergingSet} instance is a part of corresponding
 * {@link MergingWorkPackage}.
 *
 *
 */
class MergingSet {

    /**
     * state of processing of {@link MergingSet} instance
     */
    enum State {
        /**
         * The {@link MergingSet} has been declared (created).
         * No processing has been started on the bam files
         * from this set. No processing is planed to be started.
         * Enables manual selection of merging sets
         * to be processed (more control).
         */
        DECLARED,
        /**
         * Flag to be used by workflows to start processing of
         * files of this merging set
         */
        NEEDS_PROCESSING,
        /**
         * Files of this merging set are being processed (merged)
         */
        INPROGRESS,
        /**
         * Files of this merging set has been processed (merged)
         */
        PROCESSED
    }

    /**
     * identifier unique within the corresponding {@link MergingWorkPackage}
     */
    int identifier

    /**
     * current {@link State} of this instance
     */
    State status = State.DECLARED

    static belongsTo = [
        mergingWorkPackage: MergingWorkPackage
    ]

    Project getProject() {
        return mergingWorkPackage.project
    }

    public boolean isLatestSet() {
        int maxIdentifier = createCriteria().get {
            eq("mergingWorkPackage", mergingWorkPackage)
            projections{
                max("identifier")
            }
        }
        return identifier == maxIdentifier
    }
}
