package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*


class GeneModel {
    ReferenceGenome referenceGenome
    String basePath
    String fileName
    String excludeFileName
    String dexSeqFileName
    String gcFileName

    // automatic timestamping
    Date dateCreated
    Date lastUpdated

    static constraints = {
        basePath unique: true, validator: { String val ->
            OtpPath.isValidRelativePath(val)
        }
        fileName validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
        excludeFileName validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
        dexSeqFileName validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
        gcFileName validator: { String val ->
            OtpPath.isValidPathComponent(val)
        }
    }

    String toString() {
        fileName
    }
}
