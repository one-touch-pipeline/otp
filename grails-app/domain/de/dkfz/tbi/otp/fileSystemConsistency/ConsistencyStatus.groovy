package de.dkfz.tbi.otp.fileSystemConsistency

import de.dkfz.tbi.otp.ngsdata.DataFile

/**
 * Class representing the status of a DataFile after a consistency check is performed.
 * Only not consistent status should be stored.
 *
 */
class ConsistencyStatus {

    enum Status {
        NO_FILE,                // path is not null but file does not exist
        NO_READ_PERMISSION,     // file exists but cannot be read
        VIEW_BY_PID_NO_FILE,    // path from view by pid is not linked to the correct file.
        SIZE_DIFFERENCE,        // file exists but the size of the file in database is different
        CONSISTENT
    }

    /**
     * Consistency Status
     */
    Status status

    /**
     * Inconsistency resolution date
     */
    Date resolvedDate

    static constraints = {
        resolvedDate(nullable: true)
    }

    static belongsTo = [
        consistencyCheck: ConsistencyCheck,
        dataFile: DataFile
    ]
}
