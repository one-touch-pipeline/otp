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

package de.dkfz.tbi.otp.utils.logging

import org.apache.commons.logging.Log
import org.apache.commons.logging.impl.SimpleLog

import static org.springframework.util.Assert.notNull

/**
 * Holds a {@link Log} instance in a {@link Thread}
 */
class LogThreadLocal {

    /**
     * The thread local holder for {@link Log}
     */
    private static final ThreadLocal<Log> jobLogHolder = new ThreadLocal<Log>()

    /**
     * return the {@link Log} connected to this {@link Thread}.
     * If no {@link Log} is connected <code>null</code> is returned.
     *
     * @return the connected {@link Log} or null, if no {@link Log} is connected
     */
    static Log getThreadLog() {
        return jobLogHolder.get()
    }

    /**
     * set the given {@link Log} to this {@link Thread}
     *
     * <p><strong>Make sure that you call {@link #removeThreadLog()} when done (best in a finally block).</strong></p>
     *
     * @param log the {@link Log} to add to this {@link Thread}
     */
    static void setThreadLog(Log log) {
        notNull(log, "The log may not be null")
        jobLogHolder.set(log)
    }

    /**
     * remove the {@link Log} connected to this {@link Thread}, if any.
     */
    static void removeThreadLog() {
        jobLogHolder.remove()
    }

    static void withThreadLog(Appendable out, Closure code) {
        assert out != null
        assert code != null
        assert threadLog == null
        threadLog = new SimpleLog('ThreadLog') {
            // The write method has been introduced in commons-logging version 1.0.4
            @Override
            protected void write(StringBuffer buffer) {
                out.append(buffer)
                out.append('\n')
            }
        }
        try {
            code()
        } finally {
            removeThreadLog()
        }
    }

    static void withThreadLog(Log log, Closure code) {
        assert log != null
        assert code != null
        assert threadLog == null

        setThreadLog(log)
        try {
            code()
        } finally {
            removeThreadLog()
        }
    }
}
