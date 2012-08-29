package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.DataFile

class FastqcProcessedFile {

    boolean fileExists = false
    boolean contentUploaded = false
    long fileSize = -1

    Date dateCreated = new Date()
    Date dateFileSystem = null

    static belongsTo = [
        dataFile: DataFile
    ]

    static constraints = {
        dateFileSystem(nullable: true)
    }
}
