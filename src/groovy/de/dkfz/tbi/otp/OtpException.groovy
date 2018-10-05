package de.dkfz.tbi.otp

/**
 * Base exception for all exceptions thrown in OTP
 */
class OtpException extends Exception {
    OtpException() {
        this("unknown")
    }

    OtpException(String message) {
        super(message)
    }

    OtpException(String message, Throwable cause) {
        super(message, cause)
    }
}
