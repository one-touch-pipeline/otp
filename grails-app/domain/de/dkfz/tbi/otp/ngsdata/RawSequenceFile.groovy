/*
 * Copyright 2011-2026 The OTP authors
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

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.dataprocessing.ParsingException
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/** This table is used externally. Please discuss a change in the team */
@ManagedEntity
abstract class RawSequenceFile implements CommentableWithProject, Entity {

    String fileName                // file name
    String pathName                // path from run folder to file
    String vbpFileName             // file name used in view-by-pid linking

    /**
     * The md5sum of the source FASTQ file, or {@code null} if the file was not created from exactly one FASTQ file.
     * <p>
     * A {@link SequenceCramFile} converted from a read pair has no single source FASTQ file, therefore it has no
     * value here; its own checksum is {@link SequenceCramFile#cramMd5sum} (see {@link #isFastqMd5sumRequired()}).
     */
    String fastqMd5sum

    /**
     * Absolute path of the directory which this data file has been imported from.
     */
    String initialDirectory

    @Deprecated
    /**
     * @deprecated OTP-2311: Redundant with seqTrack.project
     *
     * This attribute is used externally. Please discuss a change in the team
     */
    Project project = null

    /** @deprecated OTP-2311: Redundant with run.dateExecuted   */
    @Deprecated
    Date dateExecuted = null       // when the file was originally produced
    Date dateFileSystem = null     // when the file was created on LSDF
    Date dateLastChecked = null    // when fileExists was last updated

    boolean fileWithdrawn = false
    Date withdrawnDate
    String withdrawnComment

    boolean used = false           // is this file used in any seqTrack
    boolean fileExists = false     // does file exists in file system
    boolean fileLinked = false     // is the file properly linked in view-by-pid
    long fileSize = 0              // size of the file
    boolean emptyFile = false      // is the uncompressed content of the file empty

    /**
     * In paired-end sequencing, short DNA fragments with adapters at both ends get sequenced twice,
     * once in each direction, starting from the adapter. nReads is the number of reads in a single FASTQ file.
     * <p>
     * The value gets parsed out of the FastQC result file.
     * <p>
     * Typical values are:
     * <ul>
     * <li>185,000,000 in average for WGS</li>
     * <li>18,500,000 in average for WES</li>
     * </ul>
     */
    Long nReads
    /**
     * The number of base pairs in a single read stored in a FASTQ file.
     * <p>
     * The value gets parsed out of the FastQC result file.
     * <p>
     * Typical values are:
     * <ul>
     * <li>"101"</li>
     * <li>ranges are also possible e.g. "88-100", therefore of datatype String</li>
     * </ul>
     */
    String sequenceLength

    /**
     * The mate this file contains the reads of, starting with 1, or {@code null} if the file does not represent a
     * single mate.
     * <p>
     * {@code null} is valid for:
     * <ul>
     * <li>files containing all reads of the lane instead of a single mate, for example a
     * {@link SequenceCramFile} converted from a read pair (see {@link #isMateNumberRequired()})</li>
     * <li>legacy data</li>
     * </ul>
     */
    Integer mateNumber

    boolean indexFile = false

    /** @deprecated OTP-2311: Redundant with seqTrack.run   */
    @Deprecated
    Run run
    /* OTP-2311: fastqImportInstance shall be the same for all RawSequenceFiles belonging to the same
     * SeqTrack, so actually this field should be defined in the SeqTrack class. */
    FastqImportInstance fastqImportInstance
    SeqTrack seqTrack

    @Deprecated
    FileType fileType

    static belongsTo = [
            run                : Run,
            fastqImportInstance: FastqImportInstance,
            seqTrack           : SeqTrack,
            fileType           : FileType,
    ]

    static constraints = {
        fileName(blank: false, shared: "pathComponent")
        vbpFileName(blank: false, shared: "pathComponent")

        pathName(shared: "relativePath")
        fastqMd5sum nullable: true, matches: /^([0-9a-f]{32})$/, validator: { val, obj -> val != null || !obj.isFastqMd5sumRequired() }
        initialDirectory(blank: false, shared: "absolutePath")

        project nullable: true,  // Shall not be null, but legacy data exists
                validator: { Project val, RawSequenceFile obj ->
                    obj.seqTrack == null || val == obj.seqTrack.sample.individual.project
                }
        dateExecuted(nullable: true)  // Shall not be null, but legacy data exists
        dateFileSystem(nullable: true)

        run(validator: { Run val, RawSequenceFile obj -> obj.seqTrack == null || val == obj.seqTrack.run })
        seqTrack(nullable: true)  // Shall not be null, but legacy data exists

        nReads(nullable: true)
        sequenceLength nullable: true, validator: { val, obj ->
            if (val) {
                if (!(val.matches(/\d+/) || val.matches(/\d+\-\d+/))) {
                    return "invalid"
                }
            }
        }

        comment(nullable: true)

        mateNumber nullable: true,  // Shall not be null for single mate files, but legacy data exists
                min: 1, validator: { val, obj -> obj.isMateNumberValid(val) }
        dateLastChecked(nullable: true, validator: { val, obj ->
            if (!val && obj.seqTrack?.dataInstallationState == SeqTrack.DataProcessingState.FINISHED) {
                return false
            }
        })
        withdrawnDate nullable: true, validator: { val, obj ->
            return !val || obj.fileWithdrawn
        }
        withdrawnComment nullable: true
    }

    @Override
    String toString() {
        return fileName
    }

    Individual getIndividual() {
        return seqTrack.individual
    }

    Sample getSample() {
        return seqTrack.sample
    }

    SampleType getSampleType() {
        return seqTrack.sampleType
    }

    SeqType getSeqType() {
        return seqTrack.seqType
    }

    boolean isMateNumberValid(Integer value) {
        if (indexFile) {
            return value != null // no value restriction for indexFile, except that it is given
        }
        if (value != null) {
            Integer mateCount = seqTrack?.seqType?.libraryLayout?.mateCount
            if (mateCount != null && value > mateCount) {
                return false
            }
        }
        if (fileType && fileType.type == FileType.Type.SEQUENCE && fileType.vbpPath == "/sequence/") {
            if (value == null) {
                return !isMateNumberRequired()
            }
            return value == 1 || value == 2
        }
        return true
    }

    /**
     * The name of the read this file contains, for example 'R1' for the first mate or 'I1' for the first index read.
     * <p>
     * A file without {@link #mateNumber} which does not require one (see {@link #isMateNumberRequired()}) contains all
     * reads of the lane instead of a single mate, therefore it is named after all mates of the sequencing read type,
     * for example 'R1_2' for paired end data.
     */
    String getReadName() {
        String prefix = indexFile ? 'I' : 'R'
        if (mateNumber != null || mateNumberRequired) {
            return "${prefix}${mateNumber}"
        }
        Integer mateCount = seqTrack?.seqType?.libraryLayout?.mateCount
        return mateCount ? "${prefix}${(1..mateCount).join('_')}" : prefix
    }

    static Closure mapping = {
        withdrawnComment type: 'text'
        run index: "raw_sequence_file_run_idx"
        project index: "raw_sequence_file_project_idx"
        fastqImportInstance index: "raw_sequence_file_fastq_import_instance_idx"
        seqTrack index: "raw_sequence_file_seq_track_idx"
        fastqMd5sum index: 'raw_sequence_file_fastq_md5sum_idx'
        fileType index: "raw_sequence_file_file_type_idx"
        initialDirectory type: 'text'
        dateLastChecked index: 'raw_sequence_file_date_last_checked_idx'
        comment cascade: "all-delete-orphan"
    }

    long getNBasePairs() {
        assert nReads: "nReads for sequence file ${this} is not provided."
        assert sequenceLength: "Sequence length for sequence file ${this} is not provided."
        return meanSequenceLength * nReads
    }

    Long getNBasePairsOrNull() {
        if (nReads == null || sequenceLength == null) {
            return null
        }
        return NBasePairs
    }

    int getMeanSequenceLength() {
        int length
        if (!this.sequenceLength) {
            throw new ParsingException("No meanSequenceLength could be extracted since sequenceLength of ${this} is null")
        }
        try {
            length = this.sequenceLength.toInteger()
        } catch (NumberFormatException e) {
            def sum = 0
            def count = 0
            this.sequenceLength?.split("-")?.each {
                sum += it.toInteger()
                count++
            }
            assert sum: "It was not possible to extract values from the sequence length"
            length = sum / count
        }
        return length
    }

    String getFullInitialPath() {
        return "${initialDirectory}/${fileName}"
    }

    @Override
    Project getProject() {
        return this.project
    }

    abstract String getDataFormat()

    /**
     * Whether {@link #mateNumber} has to be given for this file.
     * <p>
     * It is {@code false} for file formats which are able to contain all reads of a lane in a single file, since such
     * a file represents the reads of the lane instead of a single mate.
     */
    abstract boolean isMateNumberRequired()

    /**
     * Whether {@link #fastqMd5sum} has to be given for this file.
     * <p>
     * It is {@code false} for file formats which may be created from more than one FASTQ file, since then no single
     * source md5sum exists.
     */
    abstract boolean isFastqMd5sumRequired()
}
