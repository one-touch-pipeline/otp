package de.dkfz.tbi.otp.job.processing

/**
 * Base exception for all exceptions thrown in Job System of OTP.
 */
class ProcessingException extends RuntimeException {
    ProcessingException() {
        this("unknown")
    }

    ProcessingException(String message) {
        super(message)
    }

    ProcessingException(String message, Throwable cause) {
        super(message, cause)
    }

    ProcessingException(Throwable cause) {
        super(cause)
    }
}
