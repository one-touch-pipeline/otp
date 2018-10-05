package de.dkfz.tbi.otp.job.processing

class InvalidStateException extends Exception {
    private static final long serialVersionUID = 5694021186759169444L

    InvalidStateException() {
        // TODO Auto-generated constructor stub
    }

    InvalidStateException(String message) {
        super(message)
        // TODO Auto-generated constructor stub
    }

    InvalidStateException(Throwable cause) {
        super(cause)
        // TODO Auto-generated constructor stub
    }

    InvalidStateException(String message, Throwable cause) {
        super(message, cause)
        // TODO Auto-generated constructor stub
    }
}
