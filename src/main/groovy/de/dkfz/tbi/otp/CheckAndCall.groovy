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

import grails.converters.JSON
import grails.validation.Validateable
import grails.validation.ValidationException
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException

@Slf4j
trait CheckAndCall {

    /**
     * Catch default errors for the input command and the method processing. Return default error messages and default HTTP status codes.
     * @param cmd Validateable input command
     * @param method closure method with the main logic
     * @return http error message in case of an error
     */
    def checkDefaultErrorsAndCallMethod(Validateable cmd, Closure method) {
        if (cmd.hasErrors()) {
            String errorMessage = createErrorMessageStringFromErrors(cmd.errors) ?: g.message(code: "default.message.error.notAcceptable")
            return response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), errorMessage)
        }

        try {
            method()
        } catch (OtpRuntimeException | ValidationException | AssertionError e) {
            log.error(e.localizedMessage)
            return response.sendError(HttpStatus.BAD_REQUEST.value(), g.message(code: "default.message.error.unknown"))
        }
    }

    /**
     * Catch errors for the input command and the method processing. Return the method returns if it is successful,
     * otherwise return the default detailed error messages and default HTTP status codes.
     * @param cmd Validateable input command
     * @param method closure method with the main logic
     * @return object returned by the method or http error message in case of an error
     */
    @SuppressWarnings("CatchRuntimeException") // ignored: will be removed with the old workflow system
    def checkErrorAndCallMethodReturns(Validateable cmd, Closure method) {
        if (cmd.hasErrors()) {
            String errorMessage = createErrorMessageHtmlFromErrors(cmd.errors) ?: g.message(code: "default.message.error.notAcceptable")
            return response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), errorMessage)
        }

        try {
            return method()
        } catch (ValidationException e) {
            log.debug(e.localizedMessage)
            return response.sendError(HttpStatus.BAD_REQUEST.value(), createErrorMessageHtmlFromErrors(e.errors))
        } catch (AssertionError | RuntimeException e) {
            log.error(e.localizedMessage)
            return response.sendError(HttpStatus.BAD_REQUEST.value(), e.localizedMessage)
        }
    }

    /**
     * @deprecated Is deprecated because it returns on errors a HTTP success status code. Use {@link #checkDefaultErrorsAndCallMethod} instead.
     */
    @Deprecated
    JSON checkErrorAndCallMethod(Validateable cmd, Closure method, Closure<Map> additionalSuccessReturnValues = { [:] }) {
        Map data
        if (cmd.hasErrors()) {
            data = createErrorMessage(cmd.errors)
        } else {
            try {
                method()
                data = [success: true] + additionalSuccessReturnValues()
            } catch (ValidationException e) {
                data = createErrorMessage(e.errors)
            } catch (AssertionError e) {
                data = [success: false, error: "An error occurred: ${e.localizedMessage}"]
            } catch (OtpRuntimeException e) {
                data = [success: false, error: e.localizedMessage]
            }
        }
        render data as JSON
        return data as JSON
    }

    def <T> T checkErrorAndCallMethodWithFlashMessage(Validateable cmd, String msgCode, Closure<T> method) {
        T result = null
        withForm {
            result = checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(cmd, msgCode, method)
        }.invalidToken {
            flash.message = new FlashMessage(g.message(code: "default.expired.session") as String)
            flash.cmd = cmd
        }
        return result
    }

    def <T> T checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck(Validateable cmd, String msgCode, Closure<T> method) {
        T result = null
        if (cmd && cmd.hasErrors()) {
            flash.message = new FlashMessage(g.message(code: "${msgCode}.failed") as String, cmd.errors)
            flash.cmd = cmd
       } else {
            try {
                result = method()
                flash.message = new FlashMessage(g.message(code: "${msgCode}.success") as String)
            } catch (ValidationException e) {
                flash.message = new FlashMessage(g.message(code: "${msgCode}.failed") as String, e.errors)
                flash.cmd = cmd
            } catch (AssertionError e) {
                flash.message = new FlashMessage(g.message(code: "${msgCode}.failed") as String, e.localizedMessage)
                flash.cmd = cmd
            } catch (OtpRuntimeException e) {
                flash.message = new FlashMessage(g.message(code: "${msgCode}.failed") as String, e.localizedMessage)
                flash.cmd = cmd
            }
        }
        return result
    }

    /**
     * @deprecated Is deprecated because it returns on errors a HTTP success status code.
     *
     * A helper to check first the cmd object for validation errors and do then closure in try catch block and render the result as JSON.
     *
     * In case the cmd object has errors, the errors are concerted to a json result and rendered.
     * Otherwise it execute the callable in a try catch block, which catches:
     * - {@link ValidationException}
     * - {@link NumberFormatException}
     * - {@link AssertionError}
     * - {@link RuntimeException}
     * If one of them occurs, the error is converted to an failed JSON message and rendered.
     * Other exception are not handled here.
     *
     * If no exception occurs, an success JSON is created. If the method return an Map, that would be added to the JSON respond.
     *
     * @param cmd the command o validate
     * @param method the action to do if cmd has no errors. It should return a Map, which will be added to the success
     * @return the JSON, which is already rendered to the browser
     */
    @Deprecated
    @SuppressWarnings('CatchRuntimeException')
    JSON checkErrorAndCallMethodWithExtendedMessagesAndJsonRendering(Validateable cmd, Closure<Map> method) {
        Map data
        if (cmd.hasErrors()) {
            data = createErrorMessage(cmd.errors)
        } else {
            try {
                Map additionalData = method()
                data = [
                        success: true,
                        additionalData: additionalData,
                ]
            } catch (ValidationException e) {
                data = createErrorMessage(e.errors)
            } catch (NumberFormatException e) {
                data = [
                        success: false,
                        error  : [
                                g.message(code: 'default.message.error'),
                                g.message(code: 'default.message.noNumberException'),
                        ].join('\n    '),
                ]
            } catch (AssertionError | RuntimeException e) {
                data = [
                        success: false,
                        error  : [
                                g.message(code: 'default.message.error'),
                                e.localizedMessage,
                        ].join('\n    '),
                ]
            }
        }
        render data as JSON
        return data
    }

    /**
     * @deprecated Is deprecated because it is used to return an error message with HTTP status 200.
     */
    @Deprecated
    private Map createErrorMessage(Errors errors) {
        List<String> errorMessages = []
        if (errors.errorCount == 1) {
            errorMessages << g.message(code: "default.message.error")
        } else {
            errorMessages << g.message(code: "default.message.errors", args: errors.errorCount)
        }
        errors.allErrors.each {
            errorMessages << g.message(error: it)
        }
        return [
                success: false,
                error  : errorMessages.join('\n    '),
        ]
    }

    private String createErrorMessageStringFromErrors(Errors errors) {
        List<String> errorMessages = []

        errorMessages.add(errors.errorCount == 1 ? g.message(code: "default.message.error") :
            g.message(code: "default.message.errors", args: errors.errorCount))

        errors.allErrors.each {
            errorMessages.add(g.message(error: it))
        }

        return errorMessages.join('\n    ')
    }

    private String createErrorMessageHtmlFromErrors(Errors errors) {
        if (!errors || errors.errorCount <= 0) {
            return ""
        }

        List<String> errorMessages = []

        errorMessages.add(errors.errorCount == 1 ? g.message(code: "default.message.error") :
                g.message(code: "default.message.errors", args: errors.errorCount))

        errorMessages.add("<ul>")
        errors.allErrors.each {
            errorMessages.add("<li>${g.message(error: it)}</li>")
        }
        errorMessages.add("</ul>")

        return errorMessages.join('\n')
    }
}
