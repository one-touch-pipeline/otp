package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

/**
 * This class represents library preparation kits which are used
 * for the library creation for sequencing purpose.
 */
class LibraryPreparationKit implements Entity {

    /**
     * This is supposed to be the canonical human readable name of the kit.
     * It has to contain the manufacturer + kit name + kit version
     *
     * example: 'Agilent SureSelect V4+UTRs'
     */
    String name

    String shortDisplayName

    String adapterFile

    // used for RNA workflow
    String reverseComplementAdapterSequence

    static hasMany = [importAlias : String]

    static constraints = {
        name(unique: true, blank: false)
        shortDisplayName(unique: true, blank: false)
        adapterFile nullable: true, blank: false, validator: { val ->
            if (val && !OtpPath.isValidAbsolutePath(val)) {
                return 'Not a valid file name'
            }
        }
        reverseComplementAdapterSequence nullable: true, blank: false
    }

    @Override
    String toString() {
        return name
    }
}
