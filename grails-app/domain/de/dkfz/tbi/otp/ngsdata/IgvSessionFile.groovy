package de.dkfz.tbi.otp.ngsdata

class IgvSessionFile {

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
