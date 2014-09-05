package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqTrack

abstract class AbstractBamFile {

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

    enum FileOperationStatus {
        /**
         * default value -> state of the {@link AbstractBamFile} when it is created (declared)
         * no processing has been started on the bam file
         */
        DECLARED,
        /**
         * Files with this status need to be copied, moved or deleted.
         */
        NEEDS_PROCESSING,
        /**
         * Files are in the process of being moved, copied or deleted.
         */
        INPROGRESS,
        /**
         * Files are moved, copied or deleted.
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

    /**
     * Coverage without N of the BamFile
     */
    // Has to be from Type Double so that it can be nullable
    Double coverage

    /** Time stamp of deletion */
    Date deletionDate

    QaProcessingStatus qualityAssessmentStatus = QaProcessingStatus.UNKNOWN
    QualityControl qualityControl = QualityControl.NOT_DONE

    /**
     * Whether this has been assigned to a merging set.
     */
    State status = State.DECLARED

    public abstract Set<SeqTrack> getContainedSeqTracks()
    public abstract AbstractQualityAssessment getOverallQualityAssessment()

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
        deletionDate(nullable: true)
        coverage(nullable: true)
    }

    ReferenceGenome getReferenceGenome() {
        this.containedSeqTracks*.referenceGenome?.find()
    }

    boolean isQualityAssessed() {
        status == State.PROCESSED &&
        qualityControl == QualityControl.PASSED &&
        qualityAssessmentStatus == QaProcessingStatus.FINISHED
    }
}
