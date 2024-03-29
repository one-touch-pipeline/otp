/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.utils

import grails.util.Environment
import org.grails.exceptions.reporting.DefaultStackTraceFilterer
import org.slf4j.Logger

class ExceptionUtils {

    /**
     * If in {@link Environment#PRODUCTION}, logs the exception, otherwise throws it.
     *
     * This is for uncritical exceptions which should not disrupt service in production, but should make tests fail.
     */
    static void logOrThrow(Logger log, RuntimeException e) {
        if (Environment.current == Environment.PRODUCTION) {
            log.error e.message, e
        } else {
            throw e
        }
    }

    static String getStackTrace(final Throwable t) {
        new DefaultStackTraceFilterer().filter(t)
        CharArrayWriter buffer = new CharArrayWriter()
        t.printStackTrace(new PrintWriter(buffer))
        return buffer.toString()
    }
}
