package de.dkfz.tbi.otp.dataprocessing

class AbstractBamFile {

    enum BamType {
        SORTED,
        RMDUP
    }

    enum QaProcessingStatus {
        UNKNOWN,
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED
    }

    BamType type = null
    boolean hasIndexFile = false
    boolean hasCoveragePlot = false
    boolean hasInsertSizePlot = false

    QaProcessingStatus qualityAssessmentStatus = QaProcessingStatus.UNKNOWN

    double insertSizeMean
    double insertSizeMedian
    double insertSizeRMS
    double insertSizeFractionAboveThreshold
}
