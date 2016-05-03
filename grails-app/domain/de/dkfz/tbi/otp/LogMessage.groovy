package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.utils.Entity

class LogMessage implements Entity {

    String message
    Date dateCreated

    static mapping = {
        message type: "text"
    }

}
