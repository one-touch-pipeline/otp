package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*

class ProcessedBamFile extends AbstractFileSystemBamFile implements ProcessParameterObject {

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]

    static constraints = {
        alignmentPass nullable: false, unique: true, validator: { AlignmentPass alignmentPass ->
            return alignmentPass.referenceGenome != null
        }
    }

    public SeqTrack getSeqTrack() {
        return alignmentPass.seqTrack
    }

    @Override
    public String toString() {
        return "id: ${id} " +
                "pass: ${alignmentPass.identifier} " + (isMostRecentBamFile() ? "(latest) " : "") +
                "lane: ${seqTrack.laneId} run: ${seqTrack.run.name} " +
                "<br>sample: ${sample} " +
                "seqType: ${seqType} " +
                "<br>project: ${project}"
    }

    /**
     * @return <code>true</code>, if this {@link ProcessedBamFile} is from the latest alignment
     * @see AlignmentPass#isLatestPass()
     */
    public boolean isMostRecentBamFile() {
        return alignmentPass.isLatestPass()
    }

    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return alignmentPass.workPackage
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return new HashSet<SeqTrack>([alignmentPass.seqTrack])
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        OverallQualityAssessment.createCriteria().get {
            qualityAssessmentPass {
                eq 'processedBamFile', this
            }
            order 'id', 'desc'
            maxResults 1
        }
    }
    static mapping = {
        alignmentPass index: "abstract_bam_file_alignment_pass_idx"
    }
}
