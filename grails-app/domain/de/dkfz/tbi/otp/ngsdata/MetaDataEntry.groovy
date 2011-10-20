package de.dkfz.tbi.otp.ngsdata

class MetaDataEntry {
	
	String value

	// does the value corresponds to a value 
	// known in database (eg. sample)
	
	enum Status {VALID, INVALID, UNKNOWN}
	Status status = Status.UNKNOWN

	// was this entry present in metadata file
	// or was it inferred by the OTP ?
	
	enum Source {MDFILE, SYSTEM}
	Source source

	static belongsTo = [dataFile : DataFile, key : MetaDataKey]

	static constraints = {
		key(nullable: false)
		dataFile(nullable : false)
		value(nullable: false)
	}

	String toString() {
		"${value}"
	}
}
