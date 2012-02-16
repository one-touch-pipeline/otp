package de.dkfz.tbi.otp.ngsdata

class DataFile {

    String fileName                // file name
    String pathName                // path from run folder to file or full path
    String vbpFileName             // file name used in view-by-pid linking
    String md5sum

    Project project = null;

    Date dateExecuted = null       // when the file was originally produced
    Date dateFileSystem = null     // when the file was created on LSDF
    Date dateCreated = null        // when the object was created in db

    String vbpFilePath = ""        // viev by pid structure
    //String prvFilePath = ""        // path from run name to this file (used in solid)

    boolean metaDataValid = true
    boolean fileWithdrawn = false

    boolean used = false           // is this file used in any seqTrack
    boolean fileExists = false     // does file exists in file system
    boolean fileLinked = false     // is the file properly linked
    long fileSize = 0              // size of the file


    static belongsTo = [
        run : Run,
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

        fileName(nullable: true)
        vbpFileName(nullable: true)

        fileType(nullable: true)
        pathName(nullable: true)
        md5sum(nullable: true)

        project(nullable: true)

        dateExecuted(nullable: true)
        dateFileSystem(nullable: true)
        dateCreated(nullable: true)

        run(nullable: true)
        seqTrack(nullable: true)
        mergingLog(nullable: true)
        alignmentLog(nullable: true)
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
}
