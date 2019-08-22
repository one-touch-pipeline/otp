/*
 * Copyright 2011-2019 The OTP authors
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

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

class DataFile implements Commentable, Entity {

    String fileName                // file name
    String pathName                // path from run folder to file
    String vbpFileName             // file name used in view-by-pid linking
    String md5sum
    /**
     * Absolute path of the directory which this data file has been imported from.
     */
    String initialDirectory

    /** @deprecated OTP-2311: Redundant with seqTrack.project  */
    @Deprecated
    Project project = null

    /** @deprecated OTP-2311: Redundant with run.dateExecuted  */
    @Deprecated
    Date dateExecuted = null       // when the file was originally produced
    Date dateFileSystem = null     // when the file was created on LSDF
    Date dateLastChecked = null    // when fileExists was last updated

    boolean fileWithdrawn = false

    boolean used = false           // is this file used in any seqTrack
    boolean fileExists = false     // does file exists in file system
    boolean fileLinked = false     // is the file properly linked in view-by-pid
    long fileSize = 0              // size of the file

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

    Integer mateNumber

    boolean indexFile = false

    Comment comment

    /** @deprecated OTP-2311: Redundant with seqTrack.run  */
    @Deprecated
    Run run
    /* OTP-2311: runSegment shall be the same for all DataFiles belonging to the same
     * SeqTrack, so actually this field should be defined in the SeqTrack class. */
    RunSegment runSegment
    SeqTrack seqTrack
    MergingLog mergingLog
    AlignmentLog alignmentLog
    FileType fileType

    static belongsTo = [
            run         : Run,
            runSegment  : RunSegment,
            seqTrack    : SeqTrack,
            mergingLog  : MergingLog,
            alignmentLog: AlignmentLog,
            fileType    : FileType,
    ]

    static constraints = {

        fileName(validator: { OtpPath.isValidPathComponent(it) })
        vbpFileName(validator: { OtpPath.isValidPathComponent(it) })

        pathName(validator: { it.isEmpty() || OtpPath.isValidRelativePath(it) })
        md5sum(matches: /^([0-9a-f]{32})|(n\/a)$/) //should not be n/a, but legacy data exists
        initialDirectory(blank: false, validator: { OtpPath.isValidAbsolutePath(it) })

        project nullable: true,  // Shall not be null, but legacy data exists
                validator: { Project val, DataFile obj ->
                    obj.seqTrack == null || val == obj.seqTrack.project
                }
        dateExecuted(nullable: true)  // Shall not be null, but legacy data exists
        dateFileSystem(nullable: true)

        run(validator: { Run val, DataFile obj -> obj.seqTrack == null || val == obj.seqTrack.run })
        seqTrack(nullable: true)  // Shall not be null, but legacy data exists
        mergingLog(nullable: true)
        alignmentLog(nullable: true)

        nReads(nullable: true)
        sequenceLength nullable: true, validator: { val, obj ->
            if (val) {
                if (!(val.matches(/\d+/) || val.matches(/\d+\-\d+/))) {
                    throw new RuntimeException("The sequence length of ${obj} with value ${val} is not a valid value (number or range).")
                }
            }
        }

        comment(nullable: true)

        mateNumber nullable: true,  // Shall not be null, but legacy data exists
                min: 1, validator: { val, obj ->
            if (val != null) {
                Integer mateCount = obj.seqTrack?.seqType?.libraryLayout?.mateCount
                if (mateCount != null && val > mateCount) {
                    return false
                }
            }
            if (obj.fileType && obj.fileType.type == FileType.Type.SEQUENCE && obj.fileType.vbpPath == "/sequence/") {
                return (val == 1 || val == 2)
            } else {
                return true
            }
        }
        dateLastChecked(nullable: true, validator: { val, obj ->
            if (!val && obj.seqTrack?.dataInstallationState == SeqTrack.DataProcessingState.FINISHED) {
                return false
            }
        })
    }

    String fileSizeString() {
        if (fileSize > 1e9) return String.format("%.2f GB", fileSize / 1e9)
        if (fileSize > 1e6) return String.format("%.2f MB", fileSize / 1e6)
        if (fileSize > 1e3) return String.format("%.2f kB", fileSize / 1e3)
        return fileSize
    }

    @Override
    String toString() {
        fileName
    }

    Individual getIndividual() {
        return seqTrack.individual
    }

    SampleType getSampleType() {
        return seqTrack.sampleType
    }

    SeqType getSeqType() {
        return seqTrack.seqType
    }

    String getReadName() {
        return "${indexFile ? 'I' : 'R'}${mateNumber}"
    }

    static mapping = {
        run index: "data_file_run_idx"
        runSegment index: "data_file_run_segment_idx"
        seqTrack index: "data_file_seq_track_idx"
        mergingLog index: "data_file_merging_log_idx"
        md5sum index: 'data_file_md5sum_idx'
        alignmentLog index: "data_file_alignment_log_idx"
        fileType index: "data_file_file_type_idx"
        initialDirectory type: 'text'
        dateLastChecked index: 'data_file_date_last_checked_idx'
    }


    int getMeanSequenceLength() {
        int length
        if (!this.sequenceLength) {
            throw new RuntimeException("No meanSequenceLength could be extracted since sequenceLength of ${this} is null")
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

    @Override
    Project getProject() {
        return this.project
    }
}
