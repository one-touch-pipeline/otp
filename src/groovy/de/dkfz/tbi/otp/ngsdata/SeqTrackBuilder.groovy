package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.ExomeSeqTrack.KitInfoState
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
    private ExomeSeqTrack.KitInfoState kitInfoState = ExomeSeqTrack.KitInfoState.LATER_TO_CHECK

    /**
     * For Exome, this field is required, if the {@link ExomeSeqTrack.KitInfoState} has the value
     * {@link ExomeSeqTrack.KitInfoState#KNOWN}, otherwise, it has to be <code>null</code>
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

    /**
     * Set the {@link KitInfoState} and change {@link #exomeEnrichmentKit} to <code>null</code> if
     * the kitInfoState is not {@link KitInfoState#KNOWN}
     */
    public SeqTrackBuilder setKitInfoState(ExomeSeqTrack.KitInfoState kitInfoState) {
        this.kitInfoState = kitInfoState
        if (kitInfoState != KitInfoState.KNOWN) {
            this.exomeEnrichmentKit = null
        }
        return this
    }

    /**
     * Set the {@link ExomeEnrichmentKit} and change {@link KitInfoState} to {@link KitInfoState#KNOWN}
     */
    public SeqTrackBuilder setExomeEnrichmentKit(ExomeEnrichmentKit exomeEnrichmentKit) {
        this.exomeEnrichmentKit = exomeEnrichmentKit
        this.kitInfoState =  KitInfoState.KNOWN
        return this
    }

    public SeqTrack create() {
        SeqTrack seqTrack
        if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            notNull(kitInfoState, "A seq track needs the kit info state for exome data")
            if (kitInfoState == ExomeSeqTrack.KitInfoState.KNOWN) {
                notNull(exomeEnrichmentKit, "A exome seq track needs an exome enrichment kit when kit info state is KNOWN")
            } else {
                isNull(exomeEnrichmentKit, "A exome seq track are not allowed to have an exome enrichment kit when kit info state is not KNOWN")
            }
            seqTrack = new ExomeSeqTrack()
            seqTrack.kitInfoState = kitInfoState
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
