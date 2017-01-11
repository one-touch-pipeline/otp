package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*


class ReferenceGenomeIndex implements Entity {

    ToolName toolName
    ReferenceGenome referenceGenome
    String filePath
    String indexToolVersion

    static constraints = {
        filePath unique: true, validator: { String val ->
            OtpPath.isValidAbsolutePath(val)
        }
        indexToolVersion blank: false
    }
}
