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
package de.dkfz.tbi.otp

import grails.converters.JSON
import grails.validation.Validateable
import grails.validation.ValidationException
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.user.UserException
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException

@CompileDynamic
@Slf4j
trait CheckAndCall {

    /**
     * This method is for usage in controllers, for pages that use AJAX requests (e.g. with toaster or EditorSwitch).
     * It checks a command object for validation errors and if successful, calls the passed closure.
     *
     * If an exception occurs, it is logged and a JSON error message and HTTP error code is sent to the user agent.
     * The JSON has the keys `status`, `error` and `message`, where `message` contains either the validation errors
     * as JSON array string or the exception message.
     *
     * @param cmd input command
     * @param method closure method with the main logic
     */
    void checkErrorsAndCallMethod(Validateable cmd, Closure method) {
        if (cmd.hasErrors()) {
            renderErrorJson(HttpStatus.NOT_ACCEPTABLE, createErrorMessageJson(cmd.errors))
            return
        }

        try {
            method()
        } catch (ValidationException e) {
            log.debug(e.localizedMessage)
            renderErrorJson(HttpStatus.BAD_REQUEST, createErrorMessageJson(e.errors))
        } catch (AssertionError | OtpRuntimeException | UserException e) {
            log.error(e.message, e)
            renderErrorJson(HttpStatus.BAD_REQUEST, e.localizedMessage)
        }
    }

    /**
     * This method is for usage in controllers, for pages that use "redirect after POST" with a form token.
     * It checks the form token, then behaves like {@link #checkErrorAndCallMethodWithFlashMessageWithoutTokenCheck}.
     *
     * @param cmd input command
     * @param msgCode message code prefix; "${msgCode}.success" or "${msgCode}.failed" is shown
     * @param method closure method with the main logic
     */
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

    /**
     * This method is for usage in controllers, for pages that use "redirect after POST" without a form token.
     * It checks a command object for validation errors and if successful, calls the passed closure.
     *
     * If an exception occurs, it is logged and a `FlashMessage` is stored in the flash object as `message`.
     *
     * @param cmd input command
     * @param msgCode message code prefix; "${msgCode}.success" or "${msgCode}.failed" is shown
     * @param method closure method with the main logic
     */
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
                log.debug(e.localizedMessage)
                flash.message = new FlashMessage(g.message(code: "${msgCode}.failed") as String, e.errors)
                flash.cmd = cmd
            } catch (AssertionError | OtpRuntimeException e) {
                log.error(e.message, e)
                flash.message = new FlashMessage(g.message(code: "${msgCode}.failed") as String, e.localizedMessage)
                flash.cmd = cmd
            }
        }
        return result
    }

    /**
     * Renders the error directly instead of using `response.sendError`, since the message of the error page
     * of Spring Boot is only included if `server.error.include-message` is set (e.g. by the devtools).
     */
    private void renderErrorJson(HttpStatus status, String message) {
        response.status = status.value()
        render([status: status.value(), error: status.reasonPhrase, message: message] as JSON)
    }

    private String createErrorMessageJson(Errors errors) {
        List<String> list = errors.allErrors.collect {
            g.message(error: it)
        }

        return list as JSON
    }
}
