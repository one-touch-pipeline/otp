package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Each execution of the Quality Assessment Merged Workflow on the particular data file (merged bam file) is represented as QualityAssessmentMergedPass.
 */
class QualityAssessmentMergedPass {

    static belongsTo = [
            abstractMergedBamFile: AbstractMergedBamFile
    ]
    AbstractMergedBamFile abstractMergedBamFile

    int identifier

    String description

    static constraints = {
        identifier(unique: 'abstractMergedBamFile', validator: { int val, QualityAssessmentMergedPass obj ->
            return val == 0 || !(obj.abstractMergedBamFile instanceof RoddyBamFile)
        })
        description(nullable: true)
    }

    public String toString() {
        return "QAMP ${id}: pass ${identifier} " + (latestPass ? "(latest) " : "") + "on ${abstractMergedBamFile}"
    }

    /**
     * @return Whether this is the most recent QA pass on the referenced {@link AbstractMergedBamFile}.
     */
    public boolean isLatestPass() {
        return identifier == maxIdentifier(abstractMergedBamFile)
    }

    public static Integer maxIdentifier(final AbstractMergedBamFile abstractMergedBamFile) {
        assert abstractMergedBamFile
        return QualityAssessmentMergedPass.createCriteria().get {
            eq("abstractMergedBamFile", abstractMergedBamFile)
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
        abstractMergedBamFile index: "quality_assessment_merged_pass_abstract_merged_bam_file_idx"
    }

    Project getProject() {
        return abstractMergedBamFile.project
    }

    Individual getIndividual() {
        return abstractMergedBamFile.individual
    }

    Sample getSample() {
        return abstractMergedBamFile.sample
    }

    SampleType getSampleType() {
        return abstractMergedBamFile.sampleType
    }

    MergingSet getMergingSet() {
        if(abstractMergedBamFile instanceof ProcessedMergedBamFile) {
            return abstractMergedBamFile.mergingSet
        } else {
            throw new RuntimeException("MergingSet exists only for ProcessedMergedBamFiles")
        }
    }

    MergingWorkPackage getMergingWorkPackage() {
        return abstractMergedBamFile.mergingWorkPackage
    }

    MergingPass getMergingPass() {
        if(abstractMergedBamFile instanceof ProcessedMergedBamFile) {
            return abstractMergedBamFile.mergingPass
        } else {
            throw new RuntimeException("MergingPass exists only for ProcessedMergedBamFiles")
        }
    }

    SeqType getSeqType() {
        return abstractMergedBamFile.seqType
    }

    short getProcessingPriority() {
        return project.processingPriority
    }

    ReferenceGenome getReferenceGenome() {
        return abstractMergedBamFile.referenceGenome
    }

}
