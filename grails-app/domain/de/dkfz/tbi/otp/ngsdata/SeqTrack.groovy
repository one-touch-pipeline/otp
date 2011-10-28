package de.dkfz.tbi.otp.ngsdata

class SeqTrack {

		
	int  laneId
	
	boolean hasFinalBam = false
	boolean hasOriginalBam = false
	boolean usingOriginalBam = false
	
	long nBasePairs = 0
	long nReads = 0
	int insertSize = 0
	
	
	static belongsTo = [
		run : Run,
		sample : Sample,
		seqType : SeqType,
		seqTech : SeqTech,
		seqScan : SeqScan
	]
	
	static hasMany = [
		dataFiles : DataFile,
		alignmentLog : AlignmentLog,
		seqScan : SeqScan
	]
	
	static constraints = {
		laneId()
		hasOriginalBam()
		hasFinalBam()
		usingOriginalBam()
		seqScan(nullable: true)
		seqType()
		sample()
	}
		
	String toString() {
		"${laneId} ${sample} ${seqType} "
	}
	
}
