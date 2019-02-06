package de.dkfz.tbi.otp

import grails.converters.JSON
import org.springframework.validation.FieldError

trait CheckAndCall {

    void checkErrorAndCallMethod(Serializable cmd, Closure method) {
        Map data
        if (cmd.hasErrors()) {
            data = getErrorData(cmd.errors.getFieldError())
        } else {
            method()
            data = [success: true]
        }
        render data as JSON
    }

    private Map getErrorData(FieldError errors) {
        return [success: false, error: "'${errors.rejectedValue}' is not a valid value for '${errors.field}'. Error code: '${errors.code}'"]
    }
}
