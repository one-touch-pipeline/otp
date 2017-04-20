package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*


class GeneModel implements Entity {

    static final String GENE_MODELS = "GENE_MODELS"
    static final String GENE_MODELS_EXCLUDE = "GENE_MODELS_EXCLUDE"
    static final String GENE_MODELS_DEXSEQ = "GENE_MODELS_DEXSEQ"
    static final String GENE_MODELS_GC = "GENE_MODELS_GC"


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
        dexSeqFileName nullable: true, validator: { String val ->
            !val || OtpPath.isValidPathComponent(val)
        }
        gcFileName nullable: true, validator: { String val ->
            !val || OtpPath.isValidPathComponent(val)
        }
    }

    String toString() {
        fileName
    }
}
