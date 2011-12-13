package de.dkfz.tbi.otp.ngsdata

class MergingLog {

    // quality control flag
    enum QCState {NON, PASS, BLOCK}
    QCState qcState = QCState.NON

    enum Execution {UNKNOWN, SYSTEM, UPLOAD}
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

}
