package de.dkfz.tbi.otp

class LogMessage {

    String message
    Date timestamp

    static mapping = {
        message type: "text"
    }

}
