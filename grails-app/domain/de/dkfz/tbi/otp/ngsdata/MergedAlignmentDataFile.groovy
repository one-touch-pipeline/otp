package de.dkfz.tbi.otp.ngsdata

class MergedAlignmentDataFile {

    String fileName
    String filePath

    Date createdDate = new Date()
    Date fileSystemDate

    boolean fileExists = false
    boolean indexFileExists = false 
    long fileSize = 0

    static belongsTo = [
        mergingLog: MergingLog
    ]

    static constraints = {
        fileName()
        filePath()
        createdDate()
        fileSystemDate(nullable: true)
        mergingLog()
    }
}
