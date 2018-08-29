package de.dkfz.tbi.otp.job.processing

/**
 * Base exception for all exceptions thrown in Job System of OTP.
 */
class ProcessingException extends RuntimeException {
    public ProcessingException() {
        this("unknown")
    }

    public ProcessingException(String message) {
        super(message)
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause)
    }

    public ProcessingException(Throwable cause) {
        super(cause)
    }
}
