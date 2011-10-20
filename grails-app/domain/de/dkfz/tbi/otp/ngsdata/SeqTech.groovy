package de.dkfz.tbi.otp.ngsdata

class SeqTech {

	String name   // eg. solid, illumina
	String model  // eg. HiSeq 2000 (specified only if data incompatible)
	
	static hasMany = [
		runs : Run,
		seqTracks : SeqTrack,
		seqScans : SeqScan,
		expectedFastqFiles : ExpectedSequenceFile
	]
	
	static constraints = {
		name(unique: true)
	}
	
	String toString() {
		name
	}
}
