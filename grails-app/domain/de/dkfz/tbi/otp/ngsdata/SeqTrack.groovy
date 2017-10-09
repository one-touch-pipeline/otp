package de.dkfz.tbi.otp.ngsdata
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import groovy.transform.*

import java.text.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*
/*
 * In the GUI and e-mails sent by OTP this shall be called "Lane", even if it is only part of a multiplexed physical
 * lane. An explaining tooltip should be provided. (Decided together with the OTP Product Owner on 2016-07-19.)
 */
class SeqTrack implements ProcessParameterObject, Entity {

    static final String RUN_PREFIX = "run"

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

    @TupleConstructor
    enum Problem {
        RNA_SPIKE_IN("Contamination occurred because of RNA spike in")

        final String description
    }

    String laneId
    IlseSubmission ilseSubmission
    boolean hasOriginalBam = false
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
    SoftwareTool pipelineVersion

    QualityEncoding qualityEncoding = QualityEncoding.UNKNOWN
    DataProcessingState fastqcState = DataProcessingState.UNKNOWN

    DataProcessingState dataInstallationState = DataProcessingState.NOT_STARTED

    /** Information about problems that occurred when sequencing this SeqTrack */
    Problem problem


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

    /**
     * null for all SeqTracks created before the metadata import parsed the CUSTOMER_LIBRARY column
     * empty for SeqTracks where the metadata import found the column to be missing or empty.
     */
    String libraryName
    String normalizedLibraryName

    List<LogMessage> logMessages = []

    static belongsTo = [
        Run,
        Sample,
        SeqType,
        LibraryPreparationKit,
    ]

    static hasMany = [
            logMessages: LogMessage
    ]
    static constraints = {
        laneId(validator: { OtpPath.isValidPathComponent(it) })
        hasOriginalBam()
        seqType()
        sample()
        pipelineVersion()
        // for old data and data which is sequenced by external facilities this information might not be provided.
        ilseSubmission(nullable: true)
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
        libraryName(nullable: true, validator: { String val, SeqTrack obj ->
            !val || OtpPath.isValidPathComponent(val)
        })
        normalizedLibraryName(nullable: true, validator: {String val, SeqTrack obj ->
            (val == null) ? (obj.libraryName == null) : (val == normalizeLibraryName(obj.libraryName))
        })
        problem nullable: true
    }


    static String normalizeLibraryName(String libraryName) {
        if (libraryName != null) {
            return libraryName.trim().replaceAll(/(?<!\d)0+/, '0').replaceAll(/(?<!\d)0([1-9])/, '$1').toLowerCase(Locale.ENGLISH).replaceAll(/(?:^lib(?:rary)?)|[_-]/, '')
        } else {
            return null
        }
    }


    // To be consistent on the filesystem the library value to use is create like this and not directly derived from libraryName
    String getLibraryDirectoryName() {
        return (libraryName ? "lib${normalizedLibraryName}" : "libNA")
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

    Integer getIlseId() {
        return ilseSubmission?.ilseNumber
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

    SeqPlatform getSeqPlatform() {
        return run.seqPlatform
    }

    SeqPlatformGroup getSeqPlatformGroup() {
        return seqPlatform.getSeqPlatformGroup(project, seqType)
    }

    SeqCenter getSeqCenter() {
        return run.seqCenter
    }

    Long getNReads() {
        Long nReads = 0
        Boolean isNull = false
        DataFile.findAllBySeqTrack(this).each {
            if (it.nReads == null) {
                isNull = true
            } else {
                nReads += it.nReads
            }
        }
        return isNull ? null : nReads
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
        ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(this)
    }

    List<DataFile> getDataFiles() {
        return DataFile.findAllBySeqTrack(this)
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

    String getReadGroupName() {
        Run run = run
        if (seqType.libraryLayout == SeqType.LIBRARYLAYOUT_SINGLE) {
            DataFile dataFile = exactlyOneElement(getDataFiles())
            String fileNameWithoutExtension = dataFile.vbpFileName.split(/\./).first()
            return "${RUN_PREFIX}${run.name}_${fileNameWithoutExtension}"
        } else {
            List<DataFile> dataFiles = getDataFiles()
            assert dataFiles.size() == 2
            // if the names of datafile1 and datafile2 of one seqTrack are the same, something strange happened -> should fail
            assert dataFiles[0].vbpFileName != dataFiles[1].vbpFileName
            String commonFastQFilePrefix = getLongestCommonPrefixBeforeLastUnderscore(dataFiles[0].vbpFileName, dataFiles[1].vbpFileName)
            return "${RUN_PREFIX}${run.name}_${commonFastQFilePrefix}"
        }
    }

    static String getLongestCommonPrefixBeforeLastUnderscore(String filename1, String filename2) {
        assert filename1 : "The input filename1 must not be null"
        assert filename2 : "The input filename2 must not be null"
        String commonFastqFilePrefix = StringUtils.longestCommonPrefix(filename1, filename2)
        String pattern = /^(.*)_([^_]*)$/
        def matcher = commonFastqFilePrefix =~ pattern
        if (matcher.matches()) {
            return matcher.group(1)
        } else {
            return commonFastqFilePrefix

        }
    }

    static mapping = {
        run index: "seq_track_run_idx"
        sample index: "seq_track_sample_idx"
        seqType index: "seq_track_seq_type_idx"
        seqPlatform index: "seq_track_seq_platform_idx"
        libraryPreparationKit index: "seq_track_library_preparation_kit_idx"
        normalizedLibraryName index: "seq_track_normalized_library_name_idx"
    }
}
