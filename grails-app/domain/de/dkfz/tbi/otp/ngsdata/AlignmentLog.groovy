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
	
	static hasMany = [dataFiles : DataFile]

	static constraints = {
	}
    
    static mapping = {
        dataFiles sort:'fileName'
    }
	
    String toString() {
		"${alignmentParams} ${dataFiles}"
	}
}
