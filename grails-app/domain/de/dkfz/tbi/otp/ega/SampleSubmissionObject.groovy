package de.dkfz.tbi.otp.ega

import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.utils.Entity

class SampleSubmissionObject implements Entity, SubmissionObject {

    static belongsTo = [sample: Sample]

    static constraints = {
        egaAliasName nullable: true
    }
}
