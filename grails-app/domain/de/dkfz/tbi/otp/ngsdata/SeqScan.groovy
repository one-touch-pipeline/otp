package de.dkfz.tbi.otp.ngsdata

class SeqScan {
	
	int nLanes = 0
	long nBasePairs = 0       // calculated from seqTracks
	double coverage = 0.0     // from somewhere 

	enum State {DECLARED, PROCESSING, FINISHED, OBSOLETE}
	State state  = State.DECLARED

	String seqCenters = ""
	
	// quality control 
	enum QCState {NON, PASS, BLOCK}
	QCState qcState = QCState.NON
	
	Date dateCreated = new Date()  

	static belongsTo = [
		sample : Sample,
		seqType : SeqType,
		seqTech : SeqTech,
        //alignmentParams : AlignmentParams
	]

	static hasMany   = [
		seqTracks : SeqTrack,       // input seq tracks
		mergingLogs : MergingLog    // points to merging operation and merged bam file
        //dataFiles : DataFile      // points to merged bam file(s) 
	]

	static constraints = {
		sample()
		seqType()
		nLanes()
		nBasePairs()
		coverage()
        seqCenters()
		state()
		qcState()
		//dataFiles(nullable: true)
		//alignmentParams(nullable: true)
	}
    
    static mapping = {
        seqTracks sort:'laneId'
    }
    
	String toString() {
		"${sample} ${seqType}"
	}

}
