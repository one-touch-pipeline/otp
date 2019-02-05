package de.dkfz.tbi.otp.user

class UserException extends RuntimeException {
    UserException() {
        super()
    }

    UserException(String message) {
        super(message)
    }

    UserException(String message, Throwable cause) {
        super(message, cause)
    }

    UserException(Throwable cause) {
        super(cause)
    }
}
