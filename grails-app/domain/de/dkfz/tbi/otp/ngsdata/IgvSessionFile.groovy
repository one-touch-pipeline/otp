package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class IgvSessionFile implements Entity {

    String name
    String userName
    String content

    static constraints = {
        name()
        userName(nullable: true)
        content()
    }

    static mapping = {
        content type:"text"
    }
}
