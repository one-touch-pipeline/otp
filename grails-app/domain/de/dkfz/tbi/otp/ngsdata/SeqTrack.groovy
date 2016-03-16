package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.utils.CollectionUtils

class SeqTrack implements ProcessParameterObject {

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
    long nBasePairs = 0
    long nReads = 0
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
     * If present, the value of {link #kitInfoReliability} has to be {@link kitInfoReliability#KNOWN}
     */
    LibraryPreparationKit libraryPreparationKit

    static belongsTo = [
        Run,
        Sample,
        SeqType,
        SeqPlatform,
        LibraryPreparationKit,

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

    static mapping = {
        run index: "seq_track_run_idx"
        sample index: "seq_track_sample_idx"
        seqType index: "seq_track_seq_type_idx"
        seqPlatform index: "seq_track_seq_platform_idx"
        libraryPreparationKit index: "seq_track_library_preparation_kit_idx"
    }
}
