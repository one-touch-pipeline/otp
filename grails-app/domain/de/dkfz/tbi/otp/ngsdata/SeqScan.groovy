package de.dkfz.tbi.otp.ngsdata

class SeqScan {
	
	int nLanes = 0
	long nBasePairs = 0       // calculated from seqTracks
	double coverage = 0.0     // from somewhere 

	enum State {DECLARED, PROCESSING, FINISHED, OBSOLITE}
	State state  = State.DECLARED

	// quality control 
	enum QCState {NON, PASS, BLOCK}
	QCState qcState = QCState.NON
	
	Date dateCreated = new Date()  

	static belongsTo = [
		sample : Sample,
		seqType : SeqType,
		seqTech : SeqTech,
		alignmentParams : AlignmentParams
	]

	static hasMany   = [
		seqTracks : SeqTrack,  // input seq tracks
		dataFiles : DataFile   // points to merged bam file(s) 
	]

	static constraints = {
		nLanes()
		nBasePairs()
		coverage()
		state()
		qcState()
		seqType()
		sample()
		dataFiles(nullable: true)
	}

	String toString() {
		"${sample} ${seqType} ${nBasePairs}"
	}


}
