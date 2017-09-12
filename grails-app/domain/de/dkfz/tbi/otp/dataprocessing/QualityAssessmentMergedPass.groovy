package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.hibernate.*

/**
 * Each execution of the Quality Assessment Merged Workflow on the particular data file (merged bam file) is represented as QualityAssessmentMergedPass.
 */
class QualityAssessmentMergedPass implements ProcessParameterObject, Entity {

    static belongsTo = [
            abstractMergedBamFile: AbstractMergedBamFile
    ]
    AbstractMergedBamFile abstractMergedBamFile

    int identifier

    String description

    static constraints = {
        identifier(unique: 'abstractMergedBamFile', validator: { int val, QualityAssessmentMergedPass obj ->
            return val == 0 || !(RoddyBamFile.isAssignableFrom(Hibernate.getClass(obj.abstractMergedBamFile)))
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

    @Override
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
        if (ProcessedMergedBamFile.isAssignableFrom(Hibernate.getClass(abstractMergedBamFile))) {
            return abstractMergedBamFile.mergingSet
        } else {
            throw new RuntimeException("MergingSet exists only for ProcessedMergedBamFiles")
        }
    }

    MergingWorkPackage getMergingWorkPackage() {
        return abstractMergedBamFile.mergingWorkPackage
    }

    MergingPass getMergingPass() {
        if (ProcessedMergedBamFile.isAssignableFrom(Hibernate.getClass(abstractMergedBamFile))) {
            return abstractMergedBamFile.mergingPass
        } else {
            throw new RuntimeException("MergingPass exists only for ProcessedMergedBamFiles")
        }
    }

    @Override
    SeqType getSeqType() {
        return abstractMergedBamFile.seqType
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return abstractMergedBamFile.containedSeqTracks
    }

    @Override
    short getProcessingPriority() {
        return project.processingPriority
    }

    ReferenceGenome getReferenceGenome() {
        return abstractMergedBamFile.referenceGenome
    }

}
