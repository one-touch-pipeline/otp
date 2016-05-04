package de.dkfz.tbi.otp

class LogMessage {

    String message
    Date dateCreated

    static mapping = {
        message type: "text"
    }

}
