package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class MergingLog implements Entity {

    // quality control flag
    enum QCState {NON, PASS, BLOCK}
    QCState qcState = QCState.NON

    enum Execution {UNKNOWN, SYSTEM, UPLOAD, DISCOVERY}
    Execution executedBy = Execution.UNKNOWN

    enum Status {DECLARED, PROCESSING, FINISHED}
    Status status = Status.DECLARED

    static belongsTo = [
        alignmentParams : AlignmentParams,
        seqScan : SeqScan
    ]

    static constraints = {
        alignmentParams()
        seqScan()
    }

    String toString() {
        List<DataFile> dataFiles = DataFile.findAllByMergingLog(this)
        "${alignmentParams} ${dataFiles}"
    }

    static mapping = {
        alignmentParams index: "merging_log_alignment_params_idx"
        seqScan index: "merging_log_seq_scan_idx"
    }
}
