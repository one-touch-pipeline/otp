package de.dkfz.tbi.otp.dataprocessing

class AbstractBamFile {

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

    static constraints = {
        hasMetricsFile validator: { val, obj ->
            if (obj.type == BamType.SORTED) {
                return !val
            } else {
                return true
            }
        }
    }
}
