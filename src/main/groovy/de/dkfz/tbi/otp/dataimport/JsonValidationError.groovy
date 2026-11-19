/*
 * Copyright 2011-2026 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataimport

import groovy.transform.Immutable
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

/**
 * One error found in a JSON value, at one location.
 *
 * The location is a JSON pointer with the prefix "#", e.g. "#/properties/project" ("#" is the root), or for an invalid JSON
 * the position in the text, e.g. "line 2, column 5".
 */
@Immutable
class JsonValidationError {

    /** the error code without the prefix of the domain class and the field, e.g. {@link DataImportSchemaValidator#INVALID_JSON} */
    String code

    String location

    /** further information, e.g. the message of the JSON schema validator, or null */
    String detail

    /**
     * Adds each error as a separate {@link FieldError} to the field.
     *
     * The codes are "objectName.field.code" and "code" (as Grails creates them for the code returned by a custom validator,
     * so that the message is defined by "objectName.field.code") and {@link FieldError#getCode} returns the plain code.
     * The arguments of the message are the location ({0}) and the details ({1}).
     *
     * @param jsonErrors the errors to add, each becomes one {@link FieldError}; nothing is added for an empty list
     * @param errors the errors of the domain object, as given to a custom validator with three parameters
     * @param objectName the property name of the domain class, e.g. "dataSet"
     * @param field the name of the validated property, e.g. "rawJsonData"
     * @param rejectedValue the value of the validated property, stored as rejected value in each {@link FieldError}
     */
    static void rejectAll(List<JsonValidationError> jsonErrors, Errors errors, String objectName, String field, Object rejectedValue) {
        jsonErrors.each { JsonValidationError error ->
            String[] codes = ["${objectName}.${field}.${error.code}".toString(), error.code] as String[]
            Object[] arguments = [error.location, error.detail] as Object[]
            String defaultMessage = "${error.code} at ${error.location}${error.detail ? ": ${error.detail}" : ""}"
            ((BindingResult) errors).addError(new FieldError(errors.objectName, field, rejectedValue, false, codes, arguments, defaultMessage))
        }
    }
}
