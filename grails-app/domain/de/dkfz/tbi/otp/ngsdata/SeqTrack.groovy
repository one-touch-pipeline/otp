package de.dkfz.tbi.otp.ngsdata

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import de.dkfz.tbi.otp.utils.CollectionUtils

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

    Individual getIndividual() {
        return sample.individual
    }

    Project getProject() {
        return sample.project
    }

    SampleType getSampleType() {
        return sample.sampleType
    }

    short getProcessingPriority() {
        return project.processingPriority
    }

    /**
     * Returns the {@link ReferenceGenome} which is configured to be used for aligning this SeqTrack, or
     * <code>null</code> if it is unknown.
     * Note that the configuration may change in the future.
     */
    ReferenceGenome getConfiguredReferenceGenome() {
        switch (sampleType.specificReferenceGenome) {
            case SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT:
                return getConfiguredReferenceGenomeUsingProjectDefault()
            case SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC:
                return getConfiguredReferenceGenomeUsingSampleTypeSpecific()
            case SampleType.SpecificReferenceGenome.UNKNOWN:
                    throw new RuntimeException("For sample type '${sampleType} the way to fetch the reference genome is not defined.")
            default:
                    throw new RuntimeException("The value ${sampleType.specificReferenceGenome} for specific reference genome is not known")
        }
    }

    private ReferenceGenome getConfiguredReferenceGenomeUsingProjectDefault() {
        assert SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT == sampleType.specificReferenceGenome
        try {
            return CollectionUtils.atMostOneElement(
                            ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
                            )?.referenceGenome
        } catch (AssertionError e) {
            throw new RuntimeException("Could not find a reference genome for project '${project}' and '${seqType}'", e)
        }
    }

    private ReferenceGenome getConfiguredReferenceGenomeUsingSampleTypeSpecific() {
        assert SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC == sampleType.specificReferenceGenome
        try {
            return CollectionUtils.atMostOneElement(
                            ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeAndDeprecatedDateIsNull(project, seqType, sampleType)
                            )?.referenceGenome
        } catch (AssertionError e) {
            throw new RuntimeException("Could not find a reference genome for project '${project}' and '${seqType}' and '${sampleType}'", e)
        }
    }

    static mapping = {
        run index: "seq_track_run_idx"
        sample index: "seq_track_sample_idx"
        seqType index: "seq_track_seq_type_idx"
        seqPlatform index: "seq_track_seq_platform_idx"
    }
}
