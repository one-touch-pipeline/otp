package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.LogMessage
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject

import java.text.MessageFormat

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog

class SeqTrack implements ProcessParameterObject {

    static final Closure<? extends SeqTrack> FACTORY = { Map properties -> new SeqTrack(properties) }

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
    String ilseId
    boolean hasFinalBam = false
    boolean hasOriginalBam = false
    boolean usingOriginalBam = false
    /**
     * {@code true} if the data files belonging to this {@link SeqTrack} are symlinked to the project folder.
     * {@code false} if they are copied.
     */
    boolean linkedExternally = false
    /**
     * The number of bases of all FASTQ files belonging to a single {@link SeqTrack}
     * <p>
     * SeqTrack.nBasePairs = sum of the product {@link DataFile#nReads} * {@link DataFile#sequenceLength} of all
     * DataFiles belonging to the SeqTrack
     * <p>
     * Typical values:
     * <ul>
     * <li>38,000,000,000 in average for WGS, non-mutliplexed</li>
     * <li>3,800,000,000 in average for WES, non-multiplexed</li>
     * </ul>
     */
    Long nBasePairs
    /**
     * In paired-end sequencing the insert size describes the size in base pairs of the DNA (or RNA) fragment between two adapters.
     * Insert size = length of read 1 + length of read 2 + inner distance (unknown region between reads)
     * <p>
     * This value gets set before sequencing. Recommended values depend on the sequencing platform.
     * <p>
     * Default value is set to -1 to differentiate between unknown and 0
     * <p>
     * Typical values:
     * <ul>
     * <li>HiSeq 2000: ~400 bps</li>
     * <li>HiSeq X Ten: ~450 bps</li>
     * </ul>
     */
    int insertSize = -1
    Run run
    Sample sample
    SeqType seqType
    SeqPlatform seqPlatform
    SoftwareTool pipelineVersion

    QualityEncoding qualityEncoding = QualityEncoding.UNKNOWN
    DataProcessingState fastqcState = DataProcessingState.UNKNOWN


    /**
     * Holds the information about the state of {@link #libraryPreparationKit}.
     * If the value is {@link InformationReliability#KNOWN} or {@link InformationReliability#INFERRED},
     * the {@link #libraryPreparationKit} needs to be set. In any other case the {@link #libraryPreparationKit} has to
     * be <code>null</code>.
     * The default value is {@link InformationReliability#UNKNOWN_UNVERIFIED}
     */
    InformationReliability kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
    /**
     * Reference to the used {@link LibraryPreparationKit}.
     * If present, the value of {link #kitInfoReliability} has to be {@link InformationReliability#KNOWN}
     */
    LibraryPreparationKit libraryPreparationKit

    List<LogMessage> logMessages = []

    static belongsTo = [
        Run,
        Sample,
        SeqType,
        SeqPlatform,
        LibraryPreparationKit,

    ]

    static hasMany = [
            logMessages: LogMessage
    ]

    static constraints = {
        laneId(validator: { OtpPath.isValidPathComponent(it) })
        hasOriginalBam()
        hasFinalBam()
        usingOriginalBam()
        seqType()
        sample()
        pipelineVersion()
        // for old data and data which is sequenced from external core facilities this information might not be provided.
        ilseId nullable: true
        nBasePairs nullable: true

        //libraryPreparationKit and inferred state
        kitInfoReliability(nullable: false)
        libraryPreparationKit(nullable: true, validator: { LibraryPreparationKit val, SeqTrack obj ->
            if (obj.kitInfoReliability == InformationReliability.KNOWN || obj.kitInfoReliability == InformationReliability.INFERRED) {
                return val != null
            } else {
                return val == null
            }
        })

    }

    /**
     * @deprecated This method fails if a SeqTrack is aligned by multiple workflows or with different parameters.
     */
    @Deprecated
    DataProcessingState getAlignmentState() {
        Collection<AlignmentPass> allPasses = AlignmentPass.findAllBySeqTrack(this)
        Collection<AlignmentPass> latestPasses = allPasses.findAll( { it.isLatestPass() } )
        assert allPasses.empty == latestPasses.empty
        switch (latestPasses.size()) {
            case 0:
                return DataProcessingState.UNKNOWN
            case 1:
                return DataProcessingState.valueOf(exactlyOneElement(latestPasses).alignmentState.name())
            default:
                throw new RuntimeException(
                        "${this} is aligned by multiple workflows or with different parameters, therefore it does not have a single alignmentState.")
        }
    }

    String nBaseString() {
        return nBasePairs ? String.format("%.1f G",(nBasePairs/1e9)) : "N/A"
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
        return "ST: ${id} lane: ${laneId} run: ${run.name} " +
        "<br>sample: ${sample} seqType: ${seqType} <br>project: ${project}<br>"
    }

    /**
     * Indicates, if at least one {@link DataFile} belongs to this {@link SeqTrack}, which is marked as withdrawn
     * (see {@link DataFile#fileWithdrawn})
     */
    boolean isWithdrawn() {
        return DataFile.findBySeqTrackAndFileWithdrawn(this, true)
    }

    @Override
    Individual getIndividual() {
        return sample.individual
    }

    Project getProject() {
        return sample.project
    }

    SampleType getSampleType() {
        return sample.sampleType
    }

    SeqPlatformGroup getSeqPlatformGroup() {
        return seqPlatform.seqPlatformGroup
    }

    SeqCenter getSeqCenter() {
        return run.seqCenter
    }

    Long getNReads() {
        return DataFile.findAllBySeqTrack(this).sum { it.nReads } as Long
    }

    String getSequenceLength() {
        return exactlyOneElement(DataFile.findAllBySeqTrack(this)*.sequenceLength.unique())
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return new HashSet<SeqTrack>([this])
    }

    @Override
    short getProcessingPriority() {
        return project.processingPriority
    }

    /**
     * Returns the {@link ReferenceGenome} which is configured to be used for aligning this SeqTrack, or
     * <code>null</code> if it is unknown.
     * Note that the configuration may change in the future.
     */
    ReferenceGenome getConfiguredReferenceGenome() {
        return getConfiguredReferenceGenomeProjectSeqType()?.referenceGenome
    }

    ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType() {
        switch (sampleType.specificReferenceGenome) {
            case SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT:
                return getConfiguredReferenceGenomeProjectSeqTypeUsingProjectDefault()
            case SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC:
                return getConfiguredReferenceGenomeProjectSeqTypeUsingSampleTypeSpecific()
            case SampleType.SpecificReferenceGenome.UNKNOWN:
                    throw new RuntimeException("For sample type '${sampleType} the way to fetch the reference genome is not defined.")
            default:
                    throw new RuntimeException("The value ${sampleType.specificReferenceGenome} for specific reference genome is not known")
        }
    }

    private ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqTypeUsingProjectDefault() {
        assert SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT == sampleType.specificReferenceGenome
        try {
            return CollectionUtils.atMostOneElement(
                            ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
                            )
        } catch (AssertionError e) {
            throw new RuntimeException("Could not find a reference genome for project '${project}' and '${seqType}'", e)
        }
    }

    private ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqTypeUsingSampleTypeSpecific() {
        assert SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC == sampleType.specificReferenceGenome
        try {
            return CollectionUtils.atMostOneElement(
                            ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeAndDeprecatedDateIsNull(project, seqType, sampleType)
                            )
        } catch (AssertionError e) {
            throw new RuntimeException("Could not find a reference genome for project '${project}' and '${seqType}' and '${sampleType}'", e)
        }
    }

    public void log(String message, boolean saveInSeqTrack = true) {
        threadLog?.info(MessageFormat.format(message, " " + this))
        if (saveInSeqTrack) {
            withTransaction {
                LogMessage logMessage = new LogMessage(message: MessageFormat.format(message, ""))
                logMessage.save(flush: true, failOnError: true)
                logMessages.add(logMessage)
                this.save(flush: true, failOnError: true)
            }
        }
    }

    static mapping = {
        run index: "seq_track_run_idx"
        sample index: "seq_track_sample_idx"
        seqType index: "seq_track_seq_type_idx"
        seqPlatform index: "seq_track_seq_platform_idx"
        libraryPreparationKit index: "seq_track_library_preparation_kit_idx"
    }
}
