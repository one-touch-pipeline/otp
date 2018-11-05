package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.utils.Entity

class BamFileSubmissionObject implements Entity, SubmissionObject {

    static belongsTo = [bamFile: AbstractMergedBamFile, sampleSubmissionObject: SampleSubmissionObject]

    static constraints = {
        egaAliasName nullable: true, unique: true
    }
}
