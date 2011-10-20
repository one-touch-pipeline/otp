package de.dkfz.tbi.otp.ngsdata

class SeqType {

	String name
	String libraryLayout
	String dirName

	static hasMany = [
		//dataFiles : DataFile,
		expectedFastqFiles: ExpectedSequenceFile,
		seqTracks : SeqTrack,
		seqScans : SeqScan
	]

	static constraints = {
		name(blank: false)
		libraryLayout(blank: false)
		dirName(blank: false)
	}

	String toString() {
		"${name} ${libraryLayout}"
	}	
}
