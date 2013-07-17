package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType

class ProcessedBamFile extends AbstractFileSystemBamFile {

    /**
     * This ENUM declares the different states a {@link ProcessedBamFile} can have while it is assigned to a {@link MergingSet}
     */
    enum State {
        /**
         * default value -> state of the {@link ProcessedBamFile} when it is created (declared)
         * no processing has been started on the bam file
         */
        DECLARED,
        /**
         * {@link ProcessedBamFile} should be assigned to a {@link MergingSet}
         */
        NEEDS_PROCESSING,
        /**
         * {@link ProcessedBamFile} is currently in progress to be assigned to a {@link MergingSet}
         */
        INPROGRESS,
        /**
         * {@link ProcessedBamFile} was assigned to a {@link MergingSet}
         */
        PROCESSED
    }

    /**
     * this flag stores the actual state of a {@link ProcessedBamFile}
     */
    State status = State.DECLARED

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]

    static constraints = {
        status validator: { val, obj ->
            if (val == State.NEEDS_PROCESSING) {
                if (obj.withdrawn == true || obj.type == BamType.RMDUP) {
                    return false
                }
            }
            return true
        }
    }
}
