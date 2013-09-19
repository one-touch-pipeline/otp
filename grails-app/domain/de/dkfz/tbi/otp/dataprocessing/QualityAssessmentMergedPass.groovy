package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Each execution of the Quality Assessment Merged Workflow on the particular data file (merged bam file) is represented as QualityAssessmentMergedPass.
 */
class QualityAssessmentMergedPass {

    int identifier

    String description

    static constraints = {
        description(nullable: true)
    }

    public String toString() {
        MergingWorkPackage mergingWorkPackage = processedMergedBamFile.mergingPass.mergingSet.mergingWorkPackage
        return "pass: ${identifier} on ${mergingWorkPackage.seqType} - ${mergingWorkPackage.sample}"
    }

    static belongsTo = [
        processedMergedBamFile: ProcessedMergedBamFile
    ]
}
