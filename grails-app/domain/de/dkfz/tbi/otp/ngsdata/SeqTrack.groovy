package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class SeqTrack {

    enum DataProcessingState {
        UNKNOWN,
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED,
    }

    enum QualityEncoding {
        UNKNOWN,
        PHRED,
        ILLUMINA
    }

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

    QualityEncoding qualityEncoding = QualityEncoding.UNKNOWN
    DataProcessingState alignmentState = DataProcessingState.UNKNOWN
    DataProcessingState fastqcState = DataProcessingState.UNKNOWN

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
        return "lane: ${laneId} run: ${run.name} " +
        "<br>sample: ${sample} seqType: ${seqType} <br>project: ${project}<br>"
    }

    /**
     * Indicates, if at least one {@link DataFile} belongs to this {@link SeqTrack}, which is marked as withdrawn
     * (see {@link DataFile#fileWithdrawn})
     */
    boolean isWithdrawn() {
        return DataFile.findBySeqTrackAndFileWithdrawn(this, true)
    }

    Project getProject() {
        return sample.project
    }

    /**
     * @return The {@link ReferenceGenome}, or <code>null</code> if it is unknown.
     */
    ReferenceGenome getReferenceGenome() {
        return atMostOneElement(
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndDeprecatedDateIsNull(project, seqType)
        )?.referenceGenome
    }

    static mapping = {
        run index: "seq_track_run_idx"
        sample index: "seq_track_sample_idx"
        seqType index: "seq_track_seq_type_idx"
        seqPlatform index: "seq_track_seq_platform_idx"
    }
}
