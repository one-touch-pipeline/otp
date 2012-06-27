package de.dkfz.tbi.otp.ngsdata

class SeqScan {

    int nLanes = 0
    long nBasePairs = 0       // calculated from seqTracks
    double coverage = 0.0     // from somewhere

    enum State {DECLARED, PROCESSING, FINISHED, OBSOLETE}
    State state  = State.DECLARED

    String seqCenters = ""
    String insertSize = ""
    Sample sample
    SeqPlatform seqPlatform
    SeqType seqType

    // quality control
    enum QCState {NON, PASS, BLOCK}
    QCState qcState = QCState.NON

    Date dateCreated = new Date()

    static belongsTo = [
        sample : Sample,
        seqType : SeqType,
        seqPlatform : SeqPlatform
    ]

    static constraints = {
        sample()
        seqType()
        nLanes()
        nBasePairs()
        coverage()
        state()
        seqCenters()
        qcState()
        insertSize(nullable: true)
    }

    String toString() {
        "${sample} ${seqType}"
    }

    String basePairsString() {
        return String.format("%.1f G",(nBasePairs/1e9))
    }

    boolean isMerged() {
        return (MergingLog.countBySeqScan(this) != 0)
    }

}
