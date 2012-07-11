package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.OtpException

/**
 * Exception to indicate an error with the Changelog.
 *
 */
class ChangelogException extends OtpException {

    public ChangelogException() {
        super()
    }

    public ChangelogException(String message) {
        super(message);
    }

    public ChangelogException(String message, Throwable cause) {
        super(message, cause);
    }

}
