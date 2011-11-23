package de.dkfz.tbi.otp.ngsdata

class Individual {
	
	String pid                 // real pid from iChip
	String mockPid             // pid used in the project
	String mockFullName        // mnemonic used in the project

	enum Type {REAL, POOL, CELLLINE}
	Type type

	static belongsTo = [project : Project]
	static hasMany   = [samples : Sample]

	static constraints = {
		pid(unique: true)
	}
    
    static mapping = {
        samples sort: "type"
    }

	String toString() {
		"${mockPid} ${mockFullName}"
	}
}
