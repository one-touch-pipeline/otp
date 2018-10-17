package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*


class ReferenceGenomeIndex implements Entity {

    ToolName toolName
    ReferenceGenome referenceGenome
    // depending on the tool, path may be a file or a directory
    String path
    String indexToolVersion

    static constraints = {
        path unique: ['toolName', 'referenceGenome'], blank: false, validator: { String val ->
            OtpPath.isValidRelativePath(val)
        }
        indexToolVersion unique: ['toolName', 'referenceGenome'], blank: false
    }

    static belongsTo = [
            referenceGenome: ReferenceGenome,
    ]

    @Override
    String toString() {
        return "${referenceGenome.name} ${toolName.name} ${indexToolVersion}"
    }
}
