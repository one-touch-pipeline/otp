package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated

/**
 * Keeps all needed QA parameters for external processed merged BAM files at the moment only for Sophia
 */
class ExternalProcessedMergedBamFileQualityAssessment extends AbstractQualityAssessment implements SophiaWorkflowQualityAssessment {

    static belongsTo = QualityAssessmentMergedPass

    QualityAssessmentMergedPass qualityAssessmentMergedPass

    /**
     * insert size
     */
    @QcThresholdEvaluated
    Double insertSizeCV

    static constraints = {
        referenceLength validator: { it == null }
        insertSizeCV nullable: false
        properlyPaired nullable: false
        pairedInSequencing nullable: false
        insertSizeMedian nullable: false
    }

}
