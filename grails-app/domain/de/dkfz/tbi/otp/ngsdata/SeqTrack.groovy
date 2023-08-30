/*
 * Copyright 2011-2023 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.LogMessage
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeProjectSeqTypeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.otp.workflowExecution.Artefact

import java.util.regex.Matcher

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/*
 * In the GUI and e-mails sent by OTP this shall be called "Lane", even if it is only part of a multiplexed physical
 * lane. An explaining tooltip should be provided. (Decided together with the OTP Product Owner on 2016-07-19.)
 */
/** This table is used externally. Please discuss a change in the team */
@ManagedEntity
class SeqTrack implements ProcessParameterObject, Entity, Artefact {

    static final String RUN_PREFIX = "run"

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
    boolean lowCov = false
    /**
     * {@code true} if the data files belonging to this {@link SeqTrack} are symlinked to the project folder.
     * {@code false} if they are copied.
     *
     * This is legacy, new data will always be copied. It is only kept for old, existing data.
     */
    boolean linkedExternally = false
    /**
     * The number of bases of all FASTQ files belonging to a single {@link SeqTrack}
     * <p>
     * SeqTrack.nBasePairs = sum of the product {@link RawSequenceFile#nReads} * {@link RawSequenceFile#sequenceLength} of all
     * sequence files belonging to the SeqTrack
     * <p>
     * Typical values:
     * <ul>
     * <li>38,000,000,000 in average for WGS, non-multiplexed</li>
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
    String sampleIdentifier
    SeqType seqType
    SoftwareTool pipelineVersion
    String antibody
    AntibodyTarget antibodyTarget

    /** Indicates whether samples have been swapped on this SeqTrack */
    boolean swapped

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
     * null for all SeqTracks created before the metadata import parsed the TAGMENTATION_LIBRARY column
     * empty for SeqTracks where the metadata import found the column to be missing or empty.
     */
    String libraryName
    /** This attribute is used externally. Please discuss a change in the team */
    String normalizedLibraryName

    /**
     * For SingleCell data this contains a label for the position. This can be provide via parser
     * (as it is done for Hipo2) or by {@link MetaDataColumn#SINGLE_CELL_WELL_LABEL}.
     * currently only "well label" data used (other technologies might use different naming schema)
     */
    String singleCellWellLabel

    List<LogMessage> logMessages = []

    static belongsTo = [
            /** This attribute is used externally. Please discuss a change in the team */
            antibodyTarget       : AntibodyTarget,
            libraryPreparationKit: LibraryPreparationKit,
            run                  : Run,
            /** This attribute is used externally. Please discuss a change in the team */
            sample               : Sample,
            /** This attribute is used externally. Please discuss a change in the team */
            seqType              : SeqType,
    ]

    static hasMany = [
            logMessages: LogMessage,
    ]

    static constraints = {
        laneId blank: false, validator: { String val, SeqTrack obj ->
            // custom unique constraint on laneId, run, singleCellWellLabel and project
            List<SeqTrack> seqTracks = findAllWhere([
                    "laneId"             : obj.laneId,
                    "run"                : obj.run,
                    "singleCellWellLabel": obj.singleCellWellLabel,
            ]).findAll { SeqTrack seqTrack ->
                seqTrack != obj && seqTrack?.sample?.individual?.project == obj?.individual?.project
            }
            if (seqTracks) {
                return 'default.not.unique.message'
            }
            if (!OtpPathValidator.isValidPathComponent(val)) {
                return 'validator.path.component'
            }
        }

        sampleIdentifier nullable: false, blank: false, minSize: 3

        // for old data and data which is sequenced by external facilities this information might not be provided.
        ilseSubmission nullable: true

        nBasePairs nullable: true

        // libraryPreparationKit and inferred state
        kitInfoReliability nullable: false

        libraryPreparationKit nullable: true, validator: { LibraryPreparationKit val, SeqTrack obj ->
            if (obj.kitInfoReliability == InformationReliability.KNOWN || obj.kitInfoReliability == InformationReliability.INFERRED) {
                return val != null
            }
            return val == null
        }

        libraryName nullable: true, shared: "pathComponent"

        normalizedLibraryName nullable: true, validator: { String val, SeqTrack obj ->
            (val == null) ? (obj.libraryName == null) : (val == normalizeLibraryName(obj.libraryName))
        }

        singleCellWellLabel nullable: true

        problem nullable: true

        antibody nullable: true, blank: false, validator: { String val, SeqTrack obj ->
            return obj.antibodyTarget || !val
        }

        antibodyTarget nullable: true, validator: { AntibodyTarget val, SeqTrack obj ->
            return !obj.seqType?.hasAntibodyTarget == !val
        }
        workflowArtefact nullable: true
    }

    static String normalizeLibraryName(String libraryName) {
        if (libraryName != null) {
            return libraryName.trim()
                    .replaceAll(/(?<!\d)0+/, '0')
                    .replaceAll(/(?<!\d)0([1-9])/, '$1').toLowerCase(Locale.ENGLISH)
                    .replaceAll(/(?:^lib(?:rary)?)|[_-]/, '')
        }
        return null
    }

    // To be consistent on the filesystem the library value to use is created like this and not directly derived from libraryName
    String getLibraryDirectoryName() {
        return (libraryName ? "lib${normalizedLibraryName}" : "libNA")
    }

    String nBaseString() {
        return nBasePairs ? String.format("%.1f G", (nBasePairs / 1e9)) : "N/A"
    }

    @Override
    String toString() {
        return "ST: ${id} lane: ${laneId} run: ${run.name} <br>sample: ${sample} seqType: ${seqType} <br>project: ${project}<br>"
    }

    /**
     * Indicates, if at least one {@link RawSequenceFile} belongs to this {@link SeqTrack}, which is marked as withdrawn
     * (see {@link RawSequenceFile#fileWithdrawn})
     */
    boolean isWithdrawn() {
        return RawSequenceFile.findAllBySeqTrackAndFileWithdrawn(this, true)
    }

    Integer getIlseId() {
        return ilseSubmission?.ilseNumber
    }

    @Override
    Individual getIndividual() {
        return sample.individual
    }

    @Override
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
        return seqPlatform.getSeqPlatformGroupForMergingCriteria(project, seqType)
    }

    SeqCenter getSeqCenter() {
        return run.seqCenter
    }

    Long getNReads() {
        Long nReads = 0
        Boolean isNull = false
        sequenceFilesWhereIndexFileIsFalse.each {
            if (it.nReads == null) {
                isNull = true
            } else {
                nReads += it.nReads
            }
        }
        return isNull ? null : nReads
    }

    String getSequenceLength() {
        return exactlyOneElement(sequenceFilesWhereIndexFileIsFalse*.sequenceLength.unique())
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return new HashSet<SeqTrack>([this])
    }

    /**
     * Returns the {@link ReferenceGenome} which is configured to be used for aligning this SeqTrack, or
     * <code>null</code> if it is unknown.
     * Note that the configuration may change in the future.
     */
    ReferenceGenome getConfiguredReferenceGenome() {
        return configuredReferenceGenomeProjectSeqType?.referenceGenome
    }

    ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType() {
        return ReferenceGenomeProjectSeqTypeService.getConfiguredReferenceGenomeProjectSeqType(this)
    }

    List<RawSequenceFile> getSequenceFiles() {
        return RawSequenceFile.findAllBySeqTrack(this)
    }

    List<RawSequenceFile> getSequenceFilesWhereIndexFileIsFalse() {
        return RawSequenceFile.findAllBySeqTrackAndIndexFile(this, false)
    }

    /**
     * @deprecated Can't use save in a domain object, use SeqTrackService.logToSeqTrack() instead
     */
    @Deprecated
    void log(String message, boolean saveInSeqTrack = true) {
        SeqTrackService.logToSeqTrack(this, message, saveInSeqTrack)
    }

    String getReadGroupName() {
        if (seqType.libraryLayout == SequencingReadType.SINGLE) {
            RawSequenceFile rawSequenceFile = exactlyOneElement(sequenceFilesWhereIndexFileIsFalse)
            String fileNameWithoutExtension = rawSequenceFile.vbpFileName.split(/\./).first()
            return "${RUN_PREFIX}${run.name}_${fileNameWithoutExtension}"
        }
        List<RawSequenceFile> rawSequenceFiles = sequenceFilesWhereIndexFileIsFalse
        assert rawSequenceFiles.size() == 2
        // if the names of datafile1 and datafile2 of one seqTrack are the same, something strange happened -> should fail
        assert rawSequenceFiles[0].vbpFileName != rawSequenceFiles[1].vbpFileName
        String commonFastQFilePrefix = getLongestCommonPrefixBeforeLastUnderscore(rawSequenceFiles[0].vbpFileName, rawSequenceFiles[1].vbpFileName)
        return "${RUN_PREFIX}${run.name}_${commonFastQFilePrefix}"
    }

    static String getLongestCommonPrefixBeforeLastUnderscore(String filename1, String filename2) {
        assert filename1: "The input filename1 must not be null"
        assert filename2: "The input filename2 must not be null"
        String commonFastqFilePrefix = StringUtils.longestCommonPrefix(filename1, filename2)
        String pattern = /^(.*)_([^_]*)$/
        Matcher matcher = commonFastqFilePrefix =~ pattern
        if (matcher.matches()) {
            return matcher.group(1)
        }
        return commonFastqFilePrefix
    }

    long totalFileSize() {
        return sequenceFiles.sum { it.fileSize } as Long ?: 0
    }

    static Closure mapping = {
        laneId index: "seq_track_lane_id_idx"
        pipelineVersion index: "seq_track_pipeline_version_idx"
        antibodyTarget index: "seq_track_antibody_target_idx"
        libraryPreparationKit index: "seq_track_library_preparation_kit_idx"
        normalizedLibraryName index: "seq_track_normalized_library_name_idx"
        ilseSubmission index: "seq_track_ilse_submission_idx"
        run index: "seq_track_run_idx"
        sample index: "seq_track_sample_idx"
        sampleIdentifier index: "seq_track_sample_identifier_idx"
        seqPlatform index: "seq_track_seq_platform_idx"
        seqType index: "seq_track_seq_type_idx"
        workflowArtefact index: "seq_track_workflow_artefact_idx"
    }
}
