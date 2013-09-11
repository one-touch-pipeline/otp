package de.dkfz.tbi.otp.dataprocessing

class AbstractBamFile {

    /**
     * This ENUM declares the different states a {@link AbstractBamFile} can have while it is assigned to a {@link MergingSet}
     */
    enum State {
        /**
         * default value -> state of the {@link AbstractBamFile} when it is created (declared)
         * no processing has been started on the bam file
         */
        DECLARED,
        /**
         * {@link AbstractBamFile} should be assigned to a {@link MergingSet}
         */
        NEEDS_PROCESSING,
        /**
         * {@link AbstractBamFile} is currently in progress to be assigned to a {@link MergingSet}
         */
        INPROGRESS,
        /**
         * {@link AbstractBamFile} was assigned to a {@link MergingSet}
         */
        PROCESSED
    }

    enum BamType {
        SORTED,
        MDUP,
        RMDUP
    }

    enum QaProcessingStatus {
        UNKNOWN,
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED
    }

    enum QualityControl {
        NOT_DONE, PASSED, FAILED
    }

    BamType type = null
    boolean hasIndexFile = false
    boolean hasCoveragePlot = false
    boolean hasInsertSizePlot = false
    boolean hasMetricsFile = false
    boolean withdrawn = false

    QaProcessingStatus qualityAssessmentStatus = QaProcessingStatus.UNKNOWN
    QualityControl qualityControl = QualityControl.NOT_DONE
    /**
     * this flag stores the actual state of a {@link ProcessedBamFile}
     */
    State status = State.DECLARED

    static constraints = {
        hasMetricsFile validator: { val, obj ->
            if (obj.type == BamType.SORTED) {
                return !val
            } else {
                return true
            }
        }
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
