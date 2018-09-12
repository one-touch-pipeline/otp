package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.utils.Entity

/**
 * One object of FastqcProcessedFile represents one output file
 * of "fastqc" program. It belongs to dataFile object which represents
 * original sequence file and keep track of the status: if the file exists
 * and if it content was uploaded to data base.
 */

class FastqcProcessedFile implements Entity {

    boolean fileExists = false
    boolean contentUploaded = false
    long fileSize = -1

    Date dateCreated = new Date()
    Date dateFromFileSystem = null

    static belongsTo = [
        dataFile: DataFile,
    ]

    static constraints = {
        dateFromFileSystem(nullable: true)
        dataFile(unique: true)
    }

    static mapping = {
        dataFile index: "fastqc_processed_file_data_file_idx"
    }
}
