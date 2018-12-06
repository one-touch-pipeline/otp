package de.dkfz.tbi.otp.dataprocessing

/**
 * Keeps all needed QA parameters for Sophia workflow
 */
trait SophiaWorkflowQualityAssessment {

    abstract Long getProperlyPaired()

    abstract Double getInsertSizeMedian()

    abstract Long getPairedInSequencing()

    abstract Double getInsertSizeCV()

    abstract Double getPercentProperlyPaired()
}
