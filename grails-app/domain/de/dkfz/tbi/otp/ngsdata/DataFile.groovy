package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.dataprocessing.OtpPath

class DataFile implements Commentable{

    String fileName                // file name
    String pathName                // path from run folder to file or full path
    String vbpFileName             // file name used in view-by-pid linking
    String md5sum

    Project project = null;

    Date dateExecuted = null       // when the file was originally produced
    Date dateFileSystem = null     // when the file was created on LSDF
    Date dateCreated = null        // when the object was created in db

    String vbpFilePath = ""        // viev by pid structure

    boolean metaDataValid = true
    boolean fileWithdrawn = false

    boolean used = false           // is this file used in any seqTrack
    boolean fileExists = false     // does file exists in file system
    boolean fileLinked = false     // is the file properly linked
    long fileSize = 0              // size of the file

    // number of reads
    Long nReads
    // must be String, ranges are possible, e.g. '60-90'
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

        metaDataValid()
        used()
        fileExists()
        fileLinked()

        fileName(nullable: true, validator: { it == null || OtpPath.isValidPathComponent(it) })
        vbpFileName(nullable: true, validator: { it == null || OtpPath.isValidPathComponent(it) })

        fileType(nullable: true)
        pathName(nullable: true, validator: { !it || OtpPath.isValidRelativePath(it) })
        md5sum(nullable: true, matches: /^[0-9a-f]{32}$/)

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

        mateNumber nullable: true, validator: { val, obj ->
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

    /**
     * return formated file name starting from run directory
     * @return
     */
    public String formFileName() {
        if (pathName) {
            pathName + "/" + fileName
        }
        return fileName
    }

    String toString() {
        fileName
    }

    Individual getIndividual() {
        return seqTrack.individual
    }

    static mapping = {
        run index: "data_file_run_idx"
        runSegment index: "data_file_run_segment_idx"
        seqTrack index: "data_file_seq_track_idx"
        mergingLog index: "data_file_merging_log_idx"
        alignmentLog index: "data_file_alignment_log_idx"
        fileType index: "data_file_file_type_idx"
    }
}
