package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState
import de.dkfz.tbi.otp.ngsdata.SeqTrack.QualityEncoding

/**
 * Builder for SeqTrack
 *
 * The created {@link SeqTrack} class depends on the {@link #seqType} is:
 * <ul>
 * <li> EXOME: {@link ExomeSeqTrack}</li>
 * <li> all other case: {@link SeqTrack}</li>
 * </ul>
 * Please Note: Using the builder the default values are used from this class and not from the dom.
 *
 *
 */
class SeqTrackBuilder {

    private final String laneId

    private boolean hasFinalBam = false

    private boolean hasOriginalBam = false

    private boolean usingOriginalBam = false

    private long nBasePairs = 0

    private long nReads = 0

    private int insertSize = -1

    private final Run run

    private final Sample sample

    private final SeqType seqType

    private final SeqPlatform seqPlatform

    private final SoftwareTool pipelineVersion

    private QualityEncoding qualityEncoding = QualityEncoding.UNKNOWN

    private DataProcessingState alignmentState = DataProcessingState.UNKNOWN

    private DataProcessingState fastqcState = DataProcessingState.UNKNOWN

    /**
     * For Exome, this field is also required
     */
    private ExomeEnrichmentKit exomeEnrichmentKit

    public SeqTrackBuilder(String laneId, Run run, Sample sample,
        SeqType seqType, SeqPlatform seqPlatform,
        SoftwareTool pipelineVersion) {
        super()
        notNull(laneId, "A seq track needs a lane id")
        notNull(run, "A seq track needs a run")
        notNull(sample, "A seq track needs a sample")
        notNull(seqType, "A seq track needs a seq type")
        notNull(seqPlatform, "A seq track needs a seq platform")
        notNull(pipelineVersion, "A seq track needs a pipeline version (software tool)")

        this.laneId = laneId
        this.run = run
        this.sample = sample
        this.seqType = seqType
        this.seqPlatform = seqPlatform
        this.pipelineVersion = pipelineVersion
    }

    public SeqTrackBuilder setHasFinalBam(boolean hasFinalBam) {
        this.hasFinalBam = hasFinalBam
        return this
    }

    public SeqTrackBuilder setHasOriginalBam(boolean hasOriginalBam) {
        this.hasOriginalBam = hasOriginalBam
        return this
    }

    public SeqTrackBuilder setUsingOriginalBam(boolean usingOriginalBam) {
        this.usingOriginalBam = usingOriginalBam
        return this
    }

    public SeqTrackBuilder setnBasePairs(long nBasePairs) {
        this.nBasePairs = nBasePairs
        return this
    }

    public SeqTrackBuilder setnReads(long nReads) {
        this.nReads = nReads
        return this
    }

    public SeqTrackBuilder setInsertSize(int insertSize) {
        this.insertSize = insertSize
        return this
    }

    public SeqTrackBuilder setQualityEncoding(QualityEncoding qualityEncoding) {
        this.qualityEncoding = qualityEncoding
        return this
    }

    public SeqTrackBuilder setAlignmentState(DataProcessingState alignmentState) {
        this.alignmentState = alignmentState
        return this
    }

    public SeqTrackBuilder setFastqcState(DataProcessingState fastqcState) {
        this.fastqcState = fastqcState
        return this
    }

    public SeqTrackBuilder setExomeEnrichmentKit(
                    ExomeEnrichmentKit exomeEnrichmentKit) {
        this.exomeEnrichmentKit = exomeEnrichmentKit
        return this
    }

    public SeqTrack create() {
        SeqTrack seqTrack
        if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            notNull(exomeEnrichmentKit, "A seq track needs an exome enrichment kit for exome data")
            seqTrack = new ExomeSeqTrack()
            seqTrack.exomeEnrichmentKit = exomeEnrichmentKit
        } else {
            seqTrack = new SeqTrack()
        }

        seqTrack.laneId = laneId
        seqTrack.hasFinalBam = hasFinalBam
        seqTrack.hasOriginalBam = hasOriginalBam
        seqTrack.usingOriginalBam = usingOriginalBam
        seqTrack.nBasePairs = nBasePairs
        seqTrack.nReads = nReads
        seqTrack.insertSize = insertSize
        seqTrack.run = run
        seqTrack.sample = sample
        seqTrack.seqType = seqType
        seqTrack.seqPlatform = seqPlatform ? seqPlatform : run.seqPlatform
        seqTrack.pipelineVersion = pipelineVersion
        seqTrack.qualityEncoding = qualityEncoding
        seqTrack.alignmentState = alignmentState
        seqTrack.fastqcState = fastqcState

        if (!seqTrack.validate()) {
            throw new ProcessingException("seq track could not be validated: ${seqTrack.errors}")
        }

        return seqTrack
    }
}
