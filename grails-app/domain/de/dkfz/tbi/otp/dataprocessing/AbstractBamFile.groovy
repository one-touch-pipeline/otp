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
    Boolean hasCoveragePlot = false
    Boolean hasInsertSizePlot = false

    QaProcessingStatus qualityAssessmentStatus = QaProcessingStatus.UNKNOWN

    static constraints = {
        hasCoveragePlot(nullable: true)
        hasInsertSizePlot(nullable: true)
        qualityAssessmentStatus(nullable: true)
    }
}
