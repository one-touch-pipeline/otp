package de.dkfz.tbi.otp.job.processing

/**
 * Exception thrown when logging fails
 *
 * Calling this class means that a fatal
 * exception is thrown as the application cannot
 * track its errors anymore.
 *
 *
 */
class LoggingException extends ProcessingException {
    public LoggingException() {
        this("unknown")
    }

    public LoggingException(String message) {
        super(message)
    }

    public LoggingException(String message, Throwable cause) {
        super(message, cause)
    }
}
