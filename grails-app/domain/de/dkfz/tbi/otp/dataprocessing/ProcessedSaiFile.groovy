package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ProcessedSaiFile {

    boolean fileExists
    Date dateCreated = new Date()
    Date dateFromFileSystem
    long fileSize = -1

    /** Time stamp of deletion */
    Date deletionDate

    static belongsTo = [
        alignmentPass: AlignmentPass,
        dataFile: DataFile
    ]

    static constraints = {
        dateFromFileSystem(nullable: true)
        deletionDate(nullable: true)
    }
}
