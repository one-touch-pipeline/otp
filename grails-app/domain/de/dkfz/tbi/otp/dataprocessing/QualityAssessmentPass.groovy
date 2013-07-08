package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Execution of the Quality Assessment Workflow on the particular data file is represented as QualityAssessmentPass.
 */
class QualityAssessmentPass {

    int identifier
    String description

    static constraints = {
        description(nullable: true)
    }

    public String toString() {
        return "pass: ${identifier} on processedBamFile.id: ${processedBamFile.id}"
    }

    static belongsTo = [
        processedBamFile: ProcessedBamFile
    ]
}
