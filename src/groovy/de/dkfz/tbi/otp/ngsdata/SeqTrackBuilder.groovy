package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.InformationReliability
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

    private final String ilseId

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

    private  final SequencingKit sequencingKit

    private QualityEncoding qualityEncoding = QualityEncoding.UNKNOWN

    private DataProcessingState fastqcState = DataProcessingState.UNKNOWN

    /**
     * For Exome, this field is also required
     */
    private InformationReliability informationReliability = InformationReliability.UNKNOWN_UNVERIFIED

    /**
     * For Exome, this field is required, if the {@link InformationReliability} has the value
     * {@link InformationReliability#KNOWN}, otherwise, it has to be <code>null</code>
     */
    private LibraryPreparationKit libraryPreparationKit

    /**
     * For ChipSeq, this field is optional
     */
    private String antibody

    /**
     * For ChipSeq, this field is required
     */
    private AntibodyTarget antibodyTarget

    public SeqTrackBuilder(String laneId, Run run, Sample sample,
    SeqType seqType, SeqPlatform seqPlatform,
    SoftwareTool pipelineVersion,
    SequencingKit sequencingKit = null,
    String ilseId = null) {
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
        this.sequencingKit = sequencingKit
        this.ilseId = ilseId
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

    public SeqTrackBuilder setFastqcState(DataProcessingState fastqcState) {
        this.fastqcState = fastqcState
        return this
    }

    /**
     * Set the {@link InformationReliability} and change {@link #libraryPreparationKit} to <code>null</code> if
     * the InformationReliability is not {@link InformationReliability#KNOWN}
     */
    public SeqTrackBuilder setInformationReliability(InformationReliability reliability) {
        this.informationReliability = reliability
        if (reliability != InformationReliability.KNOWN) {
            this.libraryPreparationKit = null
        }
        return this
    }

    /**
     * Set the {@link LibraryPreparationKit} and change {@link InformationReliability} to {@link InformationReliability#KNOWN}
     */
    public SeqTrackBuilder setLibraryPreparationKit(LibraryPreparationKit libraryPreparationKit) {
        this.libraryPreparationKit = libraryPreparationKit
        this.informationReliability = InformationReliability.KNOWN
        return this
    }

    public SeqTrackBuilder setAntibody(String antibody) {
        this.antibody = antibody
        return this
    }

    public SeqTrackBuilder setAntibodyTarget(AntibodyTarget antibodyTarget) {
        this.antibodyTarget = antibodyTarget
        return this
    }

    public SeqTrack create() {
        SeqTrack seqTrack
        if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            notNull(informationReliability, "A seq track needs the kitInformationReliability for exome data")
            if (informationReliability == InformationReliability.KNOWN) {
                notNull(libraryPreparationKit, "A exome seq track needs an library preparation kit when kitInformationReliability is KNOWN")
            } else {
                isNull(libraryPreparationKit, "A exome seq track are not allowed to have an library preparation kit when kitInformationReliability is not KNOWN")
            }
            seqTrack = new ExomeSeqTrack()
            seqTrack.kitInfoReliability = informationReliability
            seqTrack.libraryPreparationKit = libraryPreparationKit
        } else if (seqType.name == SeqTypeNames.CHIP_SEQ.seqTypeName) {
            notNull(antibodyTarget, "A seq track needs the antibodyTarget for ChipSeq data")
            seqTrack = new ChipSeqSeqTrack()
            seqTrack.antibodyTarget = antibodyTarget
            seqTrack.antibody = antibody
        } else {
            seqTrack = new SeqTrack()
        }

        seqTrack.laneId = laneId
        seqTrack.ilseId = ilseId
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
        seqTrack.fastqcState = fastqcState
        seqTrack.sequencingKit = sequencingKit

        if (!seqTrack.validate()) {
            throw new ProcessingException("seq track could not be validated: ${seqTrack.errors}")
        }

        return seqTrack
    }
}
