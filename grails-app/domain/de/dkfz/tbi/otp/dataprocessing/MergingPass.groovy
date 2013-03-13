package de.dkfz.tbi.otp.dataprocessing

class MergingPass {

	int identifier
	String description
	State status = State.PROCESSING

	enum State {PROCESSING, SUCCEED, FAILED}

	static belongsTo = [
		mergingSet: MergingSet
	]

	static constraints = {
		description(nullable: true)
	}
}