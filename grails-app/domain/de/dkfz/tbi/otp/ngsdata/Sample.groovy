package de.dkfz.tbi.otp.ngsdata

class Sample {
	
	enum Type {TUMOR, CONTROL, UNKNOWN}
	Type type
	String subType             // hedge for future, eg. tumor 1, tumor 2  

	static belongsTo = [individual : Individual]
	static hasMany = [
		sampleIdentifiers: SampleIdentifier,
		seqTracks : SeqTrack,
		seqScans : SeqScan
	]

	static constraints = {
		type()
		subType(nullable: true)
		seqTracks()
		seqScans()
	}

	String toString() {
		// usefulll for scaffolding
		"${individual.mockFullName} ${type}" 
	}
}
