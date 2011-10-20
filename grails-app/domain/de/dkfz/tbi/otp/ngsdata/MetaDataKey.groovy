package de.dkfz.tbi.otp.ngsdata

class MetaDataKey {
	
	String name
	static hasMany = [metaDataEntries : MetaDataEntry]
	static constraints = {
		name(unique: true)
	}
	
	String toString() {
		name
	}
}
