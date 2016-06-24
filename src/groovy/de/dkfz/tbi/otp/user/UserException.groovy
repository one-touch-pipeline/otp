package de.dkfz.tbi.otp.user

class UserException extends RuntimeException{
    public UserException() {
        super()
    }

    public UserException(String message) {
        super(message)
    }

    public UserException(String message, Throwable cause) {
        super(message, cause)
    }

    public UserException(Throwable cause) {
        super(cause)
    }
}
