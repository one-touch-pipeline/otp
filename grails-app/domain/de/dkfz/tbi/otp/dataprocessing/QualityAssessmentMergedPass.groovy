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
        return "id: ${processedMergedBamFile.id} " +
                "pass: ${identifier} " + (latestPass ? "(latest) " : "") +
                "mergingPass: ${processedMergedBamFile.mergingPass.identifier} " +
                (processedMergedBamFile.mergingPass.latestPass ? "(latest) " : "") +
                "set: ${processedMergedBamFile.mergingPass.mergingSet.identifier} " +
                (processedMergedBamFile.mergingPass.mergingSet.latestSet ? "(latest) " : "") +
                "<br>sample: ${mergingWorkPackage.sample} " +
                "seqType: ${mergingWorkPackage.seqType} " +
                "project: ${mergingWorkPackage.project}"
    }

    /**
     * @return Whether this is the most recent QA pass on the referenced {@link ProcessedMergedBamFile}.
     */
    public boolean isLatestPass() {
        int maxIdentifier = createCriteria().get {
            eq("processedMergedBamFile", processedMergedBamFile)
            projections{
                max("identifier")
            }
        }
        return identifier == maxIdentifier
    }

    static belongsTo = [
        processedMergedBamFile: ProcessedMergedBamFile
    ]

    static mapping = {
        processedMergedBamFile index: "quality_assessment_merged_pass_processed_merged_bam_file_idx"
    }

    Project getProject() {
        return processedMergedBamFile.project
    }

    MergingPass getMergingPass() {
        return processedMergedBamFile.mergingPass
    }

    MergingSet getMergingSet() {
        return processedMergedBamFile.mergingSet
    }
}
