package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.OtpException

/**
 * This exception should be thrown whenever a Group cannot be created.
 */
class GroupCreationException extends OtpException {

    GroupCreationException(String reason) {
        super(reason);
    }

}
