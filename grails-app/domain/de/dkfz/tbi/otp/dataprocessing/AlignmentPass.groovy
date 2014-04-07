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
        // seqTrack and identifier are unique
        description(nullable: true)
    }

    public String getDirectory() {
        return "pass${identifier}"
    }

    public String toString() {
        return "pass:${identifier} on run:${seqTrack.run.name} lane:${seqTrack.laneId}"
    }

    /**
     * @return <code>true</code>, if this pass is the latest for the referenced {@link SeqTrack}
     */
    public boolean isLatestPass() {
        int maxIdentifier = AlignmentPass.createCriteria().get {
            eq("seqTrack", seqTrack)
            projections{
                max("identifier")
            }
        }
        return identifier == maxIdentifier
    }

    Project getProject() {
        return seqTrack.project
    }

    Sample getSample() {
        return seqTrack.sample
    }

    SeqType getSeqType() {
        return seqTrack.seqType
    }
}
