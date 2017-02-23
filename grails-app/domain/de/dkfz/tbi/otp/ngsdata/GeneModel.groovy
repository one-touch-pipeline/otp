package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*


class GeneModel implements Entity {
    ReferenceGenome referenceGenome
    String path
    String fileName
    String excludeFileName
    String dexSeqFileName
    String gcFileName

    // automatic timestamping
    Date dateCreated
    Date lastUpdated

    static constraints = {
        path unique: 'referenceGenome', blank: false, validator: { String val ->
            OtpPath.isValidRelativePath(val)
        }
        fileName blank: false, validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
        excludeFileName blank: false, validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
        dexSeqFileName blank: false, validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
        gcFileName blank: false, validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
    }

    String toString() {
        fileName
    }
}
