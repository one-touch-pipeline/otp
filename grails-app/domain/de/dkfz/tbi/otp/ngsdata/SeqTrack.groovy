package de.dkfz.tbi.otp.ngsdata

class SeqTrack {
    String laneId
    boolean hasFinalBam = false
    boolean hasOriginalBam = false
    boolean usingOriginalBam = false
    long nBasePairs = 0
    long nReads = 0
    int insertSize = -1
    Run run
    Sample sample
    SeqType seqType
    SeqPlatform seqPlatform
    SoftwareTool pipelineVersion

    static belongsTo = [
        Run,
        Sample,
        SeqType,
        SeqPlatform
    ]

    static constraints = {
        laneId()
        hasOriginalBam()
        hasFinalBam()
        usingOriginalBam()
        seqType()
        sample()
        pipelineVersion()
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
        AlignmentLog.findAllBySeqTrack(this).each {
            text += it.alignmentParams
            text += it.executedBy
        }
        return text
    }

    String toString() {
        "${laneId} ${sample} ${seqType} "
    }
}
