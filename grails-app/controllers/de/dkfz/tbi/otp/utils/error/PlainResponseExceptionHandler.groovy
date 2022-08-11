/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.utils.error

import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

import de.dkfz.tbi.otp.utils.ExceptionUtils

/**
 * Handles PlainResponseException of a implementing Controller.
 *
 * @see InternalServerErrorPlainResponseException
 * @see ForbiddenErrorPlainResponseException
 */
@Slf4j
trait PlainResponseExceptionHandler {

    /**
     * Gets stacktrace from handled exception and call {@link PlainResponseExceptionHandler#handlePlainResponseException}
     * to render stacktrace as plain text with code 500.
     *
     * @param ex InternalServerErrorPlainResponseException which got thrown by implementing controller.
     */
    void handleInternalServerError(InternalServerErrorPlainResponseException ex) {
        String stacktrace = ExceptionUtils.getStackTrace(ex)
        handlePlainResponseException(stacktrace, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * Gets stacktrace from handled exception and call {@link PlainResponseExceptionHandler#handlePlainResponseException}
     * to render stacktrace as plain text with code 403.
     *
     * @param ex ForbiddenErrorPlainResponseException which got thrown by implementing controller.
     */
    void handleForbiddenError(ForbiddenErrorPlainResponseException ex) {
        String stacktrace = ExceptionUtils.getStackTrace(ex)
        handlePlainResponseException(stacktrace, HttpStatus.FORBIDDEN)
    }

    /**
     * Renders a plain text stacktrace response with given stacktrace and status.
     * The exception will be also logged.
     *
     * @param stackTrace which will be responded as plain text.
     * @param httpStatus code which will be used for responding.
     *
     * @see HttpStatus
     */
    void handlePlainResponseException(String stacktrace, HttpStatus httpStatus) {
        log.error("Responding with exception stacktrace:\n $stacktrace.")
        render(text: stacktrace, status: httpStatus.value(), contentType: MediaType.TEXT_PLAIN_VALUE)
    }
}
