package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

/**
 * This class represents an Antibody target which is used
 * for the library creation for chip seq sequencing purpose.
 */
class AntibodyTarget implements Entity {

    /**
     * Antibody target name.
     * The name has to be compliant with filesystem naming as is supposed to be used as part of some filepaths.
     * example: 'H3K4me1'
     */
    String name

    static hasMany = [importAlias : String]

    static constraints = {
        name(unique: true, blank: false, validator: { OtpPath.isValidPathComponent(it) })
    }
}
