package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.utils.Entity

class DataFileSubmissionObject implements Entity, SubmissionObject {

    static belongsTo = [dataFile: DataFile]

    static constraints = {
        egaAliasName nullable: true
    }
}
