package de.dkfz.tbi.otp

class Comment {

    String comment
    String author
    Date modificationDate

    static mapping = {
        comment type: "text"
    }
}
