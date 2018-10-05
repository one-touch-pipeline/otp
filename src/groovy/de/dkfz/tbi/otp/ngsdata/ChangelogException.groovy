package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * Exception to indicate an error with the Changelog.
 *
 */
class ChangelogException extends OtpException {

    ChangelogException() {
        super()
    }

    ChangelogException(String message) {
        super(message);
    }

    ChangelogException(String message, Throwable cause) {
        super(message, cause);
    }

}
