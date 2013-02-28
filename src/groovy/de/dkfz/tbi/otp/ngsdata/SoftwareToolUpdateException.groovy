package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * Exception to indicate that updating a SoftwareTool failed.
 *
 */
class SoftwareToolUpdateException extends OtpException {

    SoftwareTool softwareTool

    SoftwareToolUpdateException(SoftwareTool softwareTool) {
        super("Updating the SoftwareTool with id ${softwareTool.id} failed")
        this.softwareTool = softwareTool
    }

}
