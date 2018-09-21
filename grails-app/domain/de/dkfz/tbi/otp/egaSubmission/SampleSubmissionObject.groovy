package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.Entity

class SampleSubmissionObject implements Entity, SubmissionObject {

    SeqType seqType

    static belongsTo = [sample: Sample]

    static constraints = {
        egaAliasName nullable: true
    }
}
