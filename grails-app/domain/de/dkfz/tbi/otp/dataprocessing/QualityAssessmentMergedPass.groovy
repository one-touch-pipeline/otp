package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Each execution of the Quality Assessment Merged Workflow on the particular data file (merged bam file) is represented as QualityAssessmentMergedPass.
 */
class QualityAssessmentMergedPass {

    int identifier

    String description

    static constraints = {
        identifier(unique: 'processedMergedBamFile')
        description(nullable: true)
    }

    public String toString() {
        MergingWorkPackage mergingWorkPackage = processedMergedBamFile.mergingPass.mergingSet.mergingWorkPackage
        return "id: ${processedMergedBamFile.id} " +
                "pass: ${identifier} " + (latestPass ? "(latest) " : "") +
                "mergingPass: ${mergingPass.identifier} " + (mergingPass.latestPass ? "(latest) " : "") +
                "set: ${mergingSet.identifier} " + (mergingSet.latestSet ? "(latest) " : "") +
                "<br>sample: ${sample} " +
                "seqType: ${mergingWorkPackage.seqType} " +
                "<br>project: ${project}"
    }

    /**
     * @return Whether this is the most recent QA pass on the referenced {@link ProcessedMergedBamFile}.
     */
    public boolean isLatestPass() {
        return identifier == maxIdentifier(processedMergedBamFile)
    }

    public static Integer maxIdentifier(final ProcessedMergedBamFile processedMergedBamFile) {
        assert processedMergedBamFile
        return QualityAssessmentMergedPass.createCriteria().get {
            eq("processedMergedBamFile", processedMergedBamFile)
            projections{
                max("identifier")
            }
        }
    }

    public static int nextIdentifier(final ProcessedMergedBamFile processedMergedBamFile) {
        assert processedMergedBamFile
        final Integer maxIdentifier = maxIdentifier(processedMergedBamFile)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
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

    Individual getIndividual() {
        return processedMergedBamFile.individual
    }

    Sample getSample() {
        return processedMergedBamFile.sample
    }

    MergingSet getMergingSet() {
        return processedMergedBamFile.mergingSet
    }

    MergingPass getMergingPass() {
        return processedMergedBamFile.mergingPass
    }

    SeqType getSeqType() {
        return processedMergedBamFile.seqType
    }

    short getProcessingPriority() {
        return project.processingPriority
    }

}
