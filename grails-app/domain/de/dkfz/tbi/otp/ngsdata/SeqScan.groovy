package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

@Deprecated
class SeqScan implements Entity {

    int nLanes = 0
    Long nBasePairs       // calculated from seqTracks
    double coverage = 0.0     // from somewhere

    @Deprecated
    enum State {DECLARED, PROCESSING, FINISHED, OBSOLETE}
    State state  = State.DECLARED

    String seqCenters = ""
    String insertSize = ""
    Sample sample
    SeqPlatform seqPlatform
    SeqType seqType

    // quality control
    @Deprecated
    enum QCState {NON, PASS, BLOCK}
    QCState qcState = QCState.NON

    Date dateCreated = new Date()

    static belongsTo = [
        sample : Sample,
        seqType : SeqType,
        seqPlatform : SeqPlatform,
    ]

    static constraints = {
        insertSize(nullable: true)
        nBasePairs (nullable: true)
    }

    @Deprecated
    String toString() {
        "${sample} ${seqType}"
    }

    @Deprecated
    String basePairsString() {
        return nBasePairs ? String.format("%.1f G",(nBasePairs/1e9)) : "N/A"
    }

    @Deprecated
    boolean isMerged() {
        return (MergingLog.countBySeqScan(this) != 0)
    }

    static mapping = {
        sample index: "seq_scan_sample_idx"
        seqType index: "seq_scan_seq_type_idx"
        seqPlatform index: "seq_scan_seq_platform_idx"
    }
}
