/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp

import groovy.transform.Canonical
import groovy.transform.ToString
import org.springframework.validation.Errors

@ToString
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

    FlashMessage(String errorMessage, String error) {
        this.message = errorMessage
        this.errorObject = null
        this.errorList = [error]
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
