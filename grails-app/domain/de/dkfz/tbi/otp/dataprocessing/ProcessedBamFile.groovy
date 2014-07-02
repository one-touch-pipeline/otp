package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ProcessedBamFile extends AbstractFileSystemBamFile {

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]

    public SeqTrack getSeqTrack() {
        return alignmentPass.seqTrack
    }

    Sample getSample() {
        return alignmentPass.sample
    }

    SeqType getSeqType() {
        return alignmentPass.seqType
    }

    Project getProject() {
        return alignmentPass.project
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

    static mapping = {
        alignmentPass index: "abstract_bam_file_alignment_pass_idx"
    }
}
