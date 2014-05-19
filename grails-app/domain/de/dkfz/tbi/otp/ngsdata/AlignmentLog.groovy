package de.dkfz.tbi.otp.ngsdata

class AlignmentLog {

    // quality control flag
    enum QCState {NON, PASS, BLOCK}
    QCState qcState = QCState.NON

    enum Execution {UNKNOWN, INITIAL, SYSTEM, UPLOAD}
    Execution executedBy = Execution.UNKNOWN

    static belongsTo = [
        alignmentParams : AlignmentParams,
        seqTrack : SeqTrack
    ]

    static constraints = {
    }

    String toString() {
        List<DataFile> dataFiles = DataFile.findAllByAlignmentLog(this)
        "${alignmentParams} ${dataFiles}"
    }

    static mapping = {
        alignmentParams index: "alignment_log_alignment_param_idx"
        seqTrack index: "alignment_log_seq_track_idx"
    }
}
