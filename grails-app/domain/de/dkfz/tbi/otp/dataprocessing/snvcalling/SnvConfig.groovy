package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline

@Deprecated
class SnvConfig extends ConfigPerProjectAndSeqType {

    /**
     * In this String the complete content of the config file is stored.
     * This solution was chosen to be as flexible as possible in case the style of the config file changes.
     */
    String configuration

    /**
     * Defines which version of the external scripts has to be used for this project.
     */
    String externalScriptVersion

    static constraints = {
        configuration blank: false
        externalScriptVersion blank: false
        seqType unique: ['project', 'obsoleteDate']  // partial index: WHERE obsolete_date IS NULL
        pipeline validator: { pipeline ->
            pipeline?.name == Pipeline.Name.OTP_SNV
        }
    }

    static mapping = {
        configuration type: 'text'
    }
}
