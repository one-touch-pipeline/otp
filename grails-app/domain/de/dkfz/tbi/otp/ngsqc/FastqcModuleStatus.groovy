package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * Domain class to store the modules status from fastqc file
 *
 */
class FastqcModuleStatus {

    enum Status {
        PASS,
        WARN,
        FAIL
    }

    /**
     * Status for fastQC module
     */
    Status status

    /**
     * FastQC module
     */
    FastqcModule module

    static belongsTo = [
        fastqcProcessedFile : FastqcProcessedFile
    ]
}
