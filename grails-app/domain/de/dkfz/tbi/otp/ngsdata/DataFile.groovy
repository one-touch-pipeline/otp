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

    Project project = null;

    Date dateExecuted = null       // when the file was originally produced
    Date dateFileSystem = null     // when the file was created on LSDF
    Date dateCreated = null        // when the object was created in db

    /** @deprecated OTP no longer imports invalid metadata */ @Deprecated
    boolean fileWithdrawn = false

    boolean used = false           // is this file used in any seqTrack
    boolean fileExists = false     // does file exists in file system
    boolean fileLinked = false     // is the file properly linked
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

    Comment comment

    Run run
    RunSegment runSegment
    SeqTrack seqTrack
    MergingLog mergingLog
    AlignmentLog alignmentLog
    FileType fileType
    static belongsTo = [
        run : Run,
        runSegment : RunSegment,
        seqTrack : SeqTrack,
        mergingLog : MergingLog,
        alignmentLog : AlignmentLog,
        fileType : FileType
    ]

    static constraints = {

        used()
        fileExists()
        fileLinked()

        fileName(nullable: true, validator: { it == null || OtpPath.isValidPathComponent(it) })
        vbpFileName(nullable: true, validator: { it == null || OtpPath.isValidPathComponent(it) })

        fileType(nullable: true)
        pathName(nullable: true, validator: { !it || OtpPath.isValidRelativePath(it) })
        md5sum(nullable: true, matches: /^[0-9a-f]{32}$/)
        initialDirectory(blank: false, validator: { OtpPath.isValidAbsolutePath(it) })

        project(nullable: true)

        dateExecuted(nullable: true)
        dateFileSystem(nullable: true)
        dateCreated(nullable: true)

        run(nullable: true)
        seqTrack(nullable: true)
        mergingLog(nullable: true)
        alignmentLog(nullable: true)
        runSegment(nullable: true)

        nReads(nullable: true)
        sequenceLength nullable: true, validator: { val, obj ->
            if (val) {
                if (!(val.matches(/\d+/) || val.matches(/\d+\-\d+/))) {
                    throw new RuntimeException("The sequence length of ${obj} with value ${val} is not a valid value (number or range).")
                }
            }
        }

        comment(nullable: true)

        mateNumber nullable: true, min: 1, validator: { val, obj ->
            if (val != null) {
                Integer mateCount = LibraryLayout.values().find { it.name() == obj.seqTrack?.seqType?.libraryLayout }?.mateCount
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
    }

    String fileSizeString() {

        if (fileSize > 1e9) return String.format("%.2f GB", fileSize/1e9)
        if (fileSize > 1e6) return String.format("%.2f MB", fileSize/1e6)
        if (fileSize > 1e3) return String.format("%.2f kB", fileSize/1e3)
        return fileSize
    }

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

    static mapping = {
        run index: "data_file_run_idx"
        runSegment index: "data_file_run_segment_idx"
        seqTrack index: "data_file_seq_track_idx"
        mergingLog index: "data_file_merging_log_idx"
        alignmentLog index: "data_file_alignment_log_idx"
        fileType index: "data_file_file_type_idx"
    }


    int getMeanSequenceLength() {
        int length
        try {
            length = this.sequenceLength.toInteger()
        } catch (NumberFormatException) {
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
}
