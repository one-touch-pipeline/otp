package de.dkfz.tbi.otp.ngsdata

class SeqTrack {

    String laneId
    //String subLaneLabel                // for multiplexing

    boolean hasFinalBam = false
    boolean hasOriginalBam = false
    boolean usingOriginalBam = false

    long nBasePairs = 0
    long nReads = 0
    int insertSize = -1


    Run run
    Sample sample
    SeqType seqType
    SeqTech seqTech


    /*
    static belongsTo = [
        run : Run,
        sample : Sample,
        seqType : SeqType,
        seqTech : SeqTech,
        seqScan : SeqScan
    ]
    */
    
    static belongsTo = [
        Run,
        Sample,
        SeqType,
        SeqTech,
        SeqScan
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

    static mapping = {
        dataFiles sort:'fileName'
    }

    String nBaseString() {
        return String.format("%.1f G",(nBasePairs/1e9))
    }

    String basePairsString() {
        String nbase = String.format("%.1f G",(nBasePairs/1e9))
        "${laneId} ${sample} ${nbase} ${insertSize}"
    }

    
    String alignmentLogString() {

        String text = ""
        alignmentLog.each {
            text += it.alignmentParams
            text += it.executedBy
        }

        return text
    }

	String toString() {
		"${laneId} ${sample} ${seqType} "
	}
	
}
