package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * Exception to indicate that updating a SoftwareTool failed.
 *
 */
class SoftwareToolIdentifierUpdateException extends OtpException {

    SoftwareToolIdentifier softwareToolIdentifier

    SoftwareToolIdentifierUpdateException(SoftwareToolIdentifier softwareToolIdentifier) {
        super("Updating the SoftwareToolIdentifier with id ${softwareToolIdentifier.id} failed")
        this.softwareTool = softwareTool
    }

}
