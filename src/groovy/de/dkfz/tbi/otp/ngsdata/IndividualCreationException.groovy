package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * This exception should be thrown whenever an Individual cannot be created.
 */
class IndividualCreationException extends OtpException {

    public IndividualCreationException(String reason) {
        super(reason)
    }

}
