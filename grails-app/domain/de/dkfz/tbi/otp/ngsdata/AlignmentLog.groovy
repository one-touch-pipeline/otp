package de.dkfz.tbi.otp.ngsdata

class AlignmentLog {
	
	// quality control flag
	enum QCState {NON, PASS, BLOCK}
	QCState qcState = QCState.NON

	static belongsTo = [
		alignmentParams : AlignmentParams,
		seqTrack : SeqTrack
	]
	
	static hasMany = [dataFiles : DataFile]

	static constraints = {
	}

	String toString() {
		"${alignmentParams} ${dataFiles}"
	}
}
