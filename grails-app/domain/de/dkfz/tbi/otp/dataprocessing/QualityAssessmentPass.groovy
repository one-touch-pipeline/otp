package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Each execution of the Quality Assessment Workflow on the particular data file is represented as QualityAssessmentPass.
 */
class QualityAssessmentPass {

    int identifier
    String description

    static constraints = {
        identifier(unique: 'processedBamFile')
        description(nullable: true)
    }

    public String toString() {
        return "id: ${processedBamFile.id} " +
                "pass: ${identifier} " + (latestPass ? "(latest) " : "") +
                "alignmentPass: ${processedBamFile.alignmentPass.identifier} " +
                (processedBamFile.alignmentPass.latestPass ? "(latest) " : "") +
                "<br>sample: ${processedBamFile.sample} " +
                "seqType: ${processedBamFile.seqType} " +
                "<br>project: ${processedBamFile.project}"
    }

    /**
     * @return Whether this is the most recent QA pass on the referenced {@link ProcessedBamFile}.
     */
    public boolean isLatestPass() {
        return identifier == maxIdentifier(processedBamFile)
    }

    public static Integer maxIdentifier(final ProcessedBamFile processedBamFile) {
        assert processedBamFile
        return QualityAssessmentPass.createCriteria().get {
            eq("processedBamFile", processedBamFile)
            projections{
                max("identifier")
            }
        }
    }

    public static int nextIdentifier(final ProcessedBamFile processedBamFile) {
        assert processedBamFile
        final Integer maxIdentifier = maxIdentifier(processedBamFile)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

    static belongsTo = [
        processedBamFile: ProcessedBamFile
    ]

    AlignmentPass getAlignmentPass() {
        return processedBamFile.alignmentPass
    }

    Project getProject() {
        return processedBamFile.project
    }

    short getProcessingPriority() {
        return project.processingPriority
    }

    SeqTrack getSeqTrack() {
        return processedBamFile.seqTrack
    }

    Sample getSample() {
        return processedBamFile.sample
    }

    SeqType getSeqType() {
        return processedBamFile.seqType
    }

    static mapping = {
        processedBamFile index: "quality_assessment_pass_processed_bam_file_idx"
    }
}
