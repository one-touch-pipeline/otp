package de.dkfz.tbi.otp

import groovy.transform.*
import org.springframework.validation.*

@Canonical
class FlashMessage {

    FlashMessage(String successMessage) {
        this.message = successMessage
        this.errorObject = null
        this.errorList = null
    }

    FlashMessage(String errorMessage, Errors errors) {
        this.message = errorMessage
        this.errorObject = errors
        this.errorList = null
    }

    FlashMessage(String errorMessage, List<String> errors) {
        this.message = errorMessage
        this.errorObject = null
        this.errorList = errors
    }

    /**
     *  the message should contain whether the action was successful or failed
     */
    final String message

    /**
     * if the action failed, "errorObject" should contain an Errors object as returned by domain or command object validation,
     * alternatively a list of errors can be given as list of strings in "errorList"
     */
    final Errors errorObject
    final List<String> errorList
}
