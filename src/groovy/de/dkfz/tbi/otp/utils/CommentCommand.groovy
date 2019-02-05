package de.dkfz.tbi.otp.utils

import grails.validation.Validateable

@Validateable
class CommentCommand implements Serializable {
    long id
    String comment
}
