package de.dkfz.tbi.otp.ngsdata

class DataFile {

    String fileName
    String pathName
    String md5sum

    Date dateExecuted = null       // when the file was originally produced
    Date dateFileSystem = null     // when the file was created on LSDF
    Date dateCreated = null        // when the object was created in db

    String vbpFilePath = ""        // viev by pid structure
    String prvFilePath = ""        // path from run name to this file (used in solid)

    boolean metaDataValid = true

    boolean used = false           // is this file used in any seqTrack
    boolean fileExists = false     // does file exists in file system
    long fileSize = 0              // size of the file


    static belongsTo = [
        run : Run,
        seqTrack : SeqTrack,
        mergingLog : MergingLog,
        alignmentLog : AlignmentLog,
        fileType : FileType
    ]

    static hasMany   = [metaDataEntries : MetaDataEntry]

    static constraints = {

        metaDataValid()
        used()

        fileName(nullable: true)
        fileType(nullable: true)
        pathName(nullable: true)
        md5sum(nullable: true)

        dateExecuted(nullable: true)
        dateFileSystem(nullable: true)
        dateCreated(nullable: true)

        run(nullable: true)
        seqTrack(nullable: true)
        mergingLog(nullable: true)
        alignmentLog(nullable: true)
    }

    String toString() {
        fileName
    }
}
