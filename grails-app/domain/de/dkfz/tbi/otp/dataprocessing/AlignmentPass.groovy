package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class AlignmentPass {

    int identifier
    SeqTrack seqTrack
    String description

    static belongsTo = [
        seqTrack: SeqTrack
    ]

    static constraints = {
        identifier(unique: 'seqTrack')
        description(nullable: true)
    }

    public String getDirectory() {
        return "pass${identifier}"
    }

    public String toString() {
        return "id: ${seqTrack.id} " +
                "pass: ${identifier} " + (latestPass ? "(latest) " : "") +
                "lane: ${seqTrack.laneId} run: ${seqTrack.run.name} " +
                "<br>sample: ${seqTrack.sample} " +
                "seqType: ${seqTrack.seqType} " +
                "<br>project: ${seqTrack.project}"
    }

    /**
     * @return <code>true</code>, if this pass is the latest for the referenced {@link SeqTrack}
     */
    public boolean isLatestPass() {
        return identifier == maxIdentifier(seqTrack)
    }

    public static Integer maxIdentifier(final SeqTrack seqTrack) {
        assert seqTrack
        return AlignmentPass.createCriteria().get {
            eq("seqTrack", seqTrack)
            projections {
                max("identifier")
            }
        }
    }

    public static int nextIdentifier(final SeqTrack seqTrack) {
        assert seqTrack
        final Integer maxIdentifier = maxIdentifier(seqTrack)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

    Project getProject() {
        return seqTrack.project
    }

    short getProcessingPriority() {
        return project.processingPriority
    }

    Sample getSample() {
        return seqTrack.sample
    }

    SeqType getSeqType() {
        return seqTrack.seqType
    }

    Individual getIndividual() {
        return seqTrack.individual
    }

    static mapping = {
        seqTrack index: "alignment_pass_seq_track_idx"
    }
}
