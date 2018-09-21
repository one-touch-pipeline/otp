package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.utils.Entity

class BamFileSubmissionObject implements Entity, SubmissionObject {

    static belongsTo = [bamFile: AbstractMergedBamFile]

    static constraints = {
        egaAliasName nullable: true
    }
}
