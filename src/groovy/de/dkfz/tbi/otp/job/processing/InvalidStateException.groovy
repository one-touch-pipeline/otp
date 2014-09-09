package de.dkfz.tbi.otp.job.processing

public class InvalidStateException extends Exception {
    private static final long serialVersionUID = 5694021186759169444L

    public InvalidStateException() {
        // TODO Auto-generated constructor stub
    }

    public InvalidStateException(String message) {
        super(message)
        // TODO Auto-generated constructor stub
    }

    public InvalidStateException(Throwable cause) {
        super(cause)
        // TODO Auto-generated constructor stub
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause)
        // TODO Auto-generated constructor stub
    }
}
