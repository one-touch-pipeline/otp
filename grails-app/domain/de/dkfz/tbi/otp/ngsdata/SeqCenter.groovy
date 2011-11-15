package de.dkfz.tbi.otp.ngsdata

class SeqCenter {
	
	String name
	String dirName

	static hasMany = [
		runs : Run,
		contactPerson : ContactPerson
	]

	static constraints = {
		name(blank: false, unique: true)
	}

	String toString() {
		name
	}
}
