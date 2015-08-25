package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Each execution of the Quality Assessment Merged Workflow on the particular data file (merged bam file) is represented as QualityAssessmentMergedPass.
 */
class QualityAssessmentMergedPass {

    static belongsTo = [
            processedMergedBamFile: AbstractMergedBamFile
    ]
    AbstractMergedBamFile processedMergedBamFile

    int identifier

    String description

    static constraints = {
        identifier(unique: 'processedMergedBamFile', validator: { int val, QualityAssessmentMergedPass obj ->
            return val == 0 || !(obj.processedMergedBamFile instanceof RoddyBamFile)
        })
        description(nullable: true)
    }

    public String toString() {
        return "QAMP ${id}: pass ${identifier} " + (latestPass ? "(latest) " : "") + "on ${processedMergedBamFile}"
    }

    /**
     * @return Whether this is the most recent QA pass on the referenced {@link AbstractMergedBamFile}.
     */
    public boolean isLatestPass() {
        return identifier == maxIdentifier(processedMergedBamFile)
    }

    public static Integer maxIdentifier(final AbstractMergedBamFile abstractMergedBamFile) {
        assert abstractMergedBamFile
        return QualityAssessmentMergedPass.createCriteria().get {
            eq("processedMergedBamFile", abstractMergedBamFile)
            projections{
                max("identifier")
            }
        }
    }

    public static int nextIdentifier(final AbstractMergedBamFile abstractMergedBamFile) {
        assert abstractMergedBamFile
        final Integer maxIdentifier = maxIdentifier(abstractMergedBamFile)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

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

    SampleType getSampleType() {
        return processedMergedBamFile.sampleType
    }

    MergingSet getMergingSet() {
        return processedMergedBamFile.mergingSet
    }

    MergingWorkPackage getMergingWorkPackage() {
        return processedMergedBamFile.mergingWorkPackage
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

    ReferenceGenome getReferenceGenome() {
        return processedMergedBamFile.referenceGenome
    }

}
