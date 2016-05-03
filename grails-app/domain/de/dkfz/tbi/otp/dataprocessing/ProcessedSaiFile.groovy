package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity

class ProcessedSaiFile implements Entity {

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

    Project getProject() {
        return alignmentPass.project
    }

    static mapping = {
        alignmentPass index: "processed_sai_file_alignemt_pass_idx"
        dataFile index: "processed_sai_file_data_file_idx"
    }
}
