package de.dkfz.tbi.otp.dataprocessing

class ProcessedBamFile extends AbstractBamFile {

    boolean fileExists
    Date dateCreated = new Date()
    Date dateFromFileSystem
    long fileSize = -1

    static belongsTo = [
        alignmentPass: AlignmentPass
    ]

    static constraints = {
        dateFromFileSystem(nullable: true)
    }
}
