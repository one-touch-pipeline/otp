package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class MergedAlignmentDataFile implements Entity {

    String fileSystem
    String filePath
    String fileName

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
        fileSystem()
        createdDate()
        fileSystemDate(nullable: true)
        mergingLog()
    }

    String fileSizeString() {
        if (fileSize > 1e9) return String.format("%.2f GB", fileSize/1e9)
        if (fileSize > 1e6) return String.format("%.2f MB", fileSize/1e6)
        if (fileSize > 1e3) return String.format("%.2f kB", fileSize/1e3)
        return fileSize
    }

    static mapping = {
        mergingLog index: "merged_alignment_data_file_merging_log_idx"
    }
}
