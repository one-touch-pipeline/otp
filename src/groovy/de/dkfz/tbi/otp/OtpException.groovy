package de.dkfz.tbi.otp

/**
 * Base exception for all exceptions thrown in OTP
 */
public class OtpException extends Exception {
    public OtpException() {
        this("unknown")
    }

    public OtpException(String message) {
        super(message)
    }

    public OtpException(String message, Throwable cause) {
        super(message, cause)
    }
}
