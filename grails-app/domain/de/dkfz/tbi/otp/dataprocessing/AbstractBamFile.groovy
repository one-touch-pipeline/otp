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

    enum QualityControl {NOT_DONE, PASSED, FAILED}

    BamType type = null
    boolean hasIndexFile = false
    Boolean hasCoveragePlot = false
    Boolean hasInsertSizePlot = false

    QaProcessingStatus qualityAssessmentStatus = QaProcessingStatus.UNKNOWN
    QualityControl qualityControl = QualityControl.NOT_DONE

    static constraints = {
        hasCoveragePlot(nullable: true)
        hasInsertSizePlot(nullable: true)
        qualityAssessmentStatus(nullable: true)
    }
}
