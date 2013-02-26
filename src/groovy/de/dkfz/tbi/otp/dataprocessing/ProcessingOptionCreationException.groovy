package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.OtpException

/**
 * This exception should be thrown whenever a ProcessingOption cannot be created.
 *
 */
class ProcessingOptionCreationException extends OtpException {

    public ProcessingOptionCreationException(String reason) {
        super(reason)
    }

}
