/*
 * Copyright 2011-2021 The OTP authors
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

import groovy.util.logging.Slf4j
import org.slf4j.Logger

/**
 * Utility to calculate the time needed for execute a given closure
 */
@Slf4j
class LogUsedTimeUtils {

    /**
     * utility to log the time used for execute the closure
     *
     * @param logText a text used for logging the time
     * @param closure the closure for which the duration should be taken
     * @return the value of the closure
     */
    static <T> T logUsedTime(String logText, Closure<T> closure) {
        long start = System.currentTimeMillis()
        T t = closure()
        long end = System.currentTimeMillis()
        log.debug("${logText}: ${end - start}ms")
        return t
    }

    /**
     * utility to log the time used for execute the closure using the provided logger
     *
     * @param logger the logger to use
     * @param logText a text used for logging the time
     * @param closure the closure for which the duration should be taken
     * @return the value of the closure
     */
    static <T> T logUsedTime(Logger logger, String logText, Closure<T> closure) {
        long start = System.currentTimeMillis()
        T t = closure()
        long end = System.currentTimeMillis()
        logger.debug("${logText}: took ${end - start}ms")
        return t
    }

    /**
     * Utility to log the start and the time used for execute the closure using the provided logger
     *
     * @param logger the logger to use
     * @param logText a text used for logging the time
     * @param closure the closure for which the duration should be taken
     * @return the value of the closure
     */
    static <T> T logUsedTimeStartEnd(Logger logger, String logText, Closure<T> closure) {
        logger.debug("${logText}: starts")
        long start = System.currentTimeMillis()
        T t = closure()
        long end = System.currentTimeMillis()
        logger.debug("${logText}: ends, took ${end - start}ms")
        return t
    }
}
