package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * Exception to indicate that updating a MetaDataEntry failed.
 */
class MetaDataEntryUpdateException extends OtpException {
    MetaDataEntry entry

    MetaDataEntryUpdateException(MetaDataEntry entry) {
        super("Updating the MetaDataEntry with id ${entry.id} failed")
        this.entry = entry
    }

}
