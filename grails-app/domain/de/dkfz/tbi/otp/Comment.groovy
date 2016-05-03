package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.utils.Entity

class Comment implements Entity {

    String comment
    String author
    Date modificationDate

    static mapping = {
        comment type: "text"
    }
}
