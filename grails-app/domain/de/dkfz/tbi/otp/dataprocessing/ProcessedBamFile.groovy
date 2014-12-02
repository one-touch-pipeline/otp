package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ProcessedBamFile extends AbstractFileSystemBamFile {

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]

    static constraints = {
        alignmentPass nullable: false, unique: true
    }

    public SeqTrack getSeqTrack() {
        return alignmentPass.seqTrack
    }

    Sample getSample() {
        return alignmentPass.sample
    }

    @Override
    SeqType getSeqType() {
        return alignmentPass.seqType
    }

    @Override
    Individual getIndividual() {
        return alignmentPass.individual
    }

    Project getProject() {
        return alignmentPass.project
    }

    short getProcessingPriority() {
        return project.processingPriority
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
