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
package de.dkfz.tbi.otp.utils.logging

import org.slf4j.*
import org.slf4j.event.Level
import org.slf4j.helpers.*

import static org.springframework.util.Assert.*

/**
 * Holds a {@link Logger} instance in a {@link Thread}
 */
class LogThreadLocal {

    /**
     * The thread local holder for {@link Logger}
     */
    private static final ThreadLocal<Logger> JOB_LOG_HOLDER = new ThreadLocal<Logger>()

    /**
     * return the {@link Logger} connected to this {@link Thread}.
     * If no {@link Logger} is connected <code>null</code> is returned.
     *
     * @return the connected {@link Logger} or null, if no {@link Logger} is connected
     */
    static Logger getThreadLog() {
        return JOB_LOG_HOLDER.get()
    }

    /**
     * set the given {@link Logger} to this {@link Thread}
     *
     * <p><strong>Make sure that you call {@link #removeThreadLog()} when done (best in a finally block).</strong></p>
     *
     * @param log the {@link Logger} to add to this {@link Thread}
     */
    static void setThreadLog(Logger log) {
        notNull(log, "The log may not be null")
        JOB_LOG_HOLDER.set(log)
    }

    /**
     * remove the {@link Logger} connected to this {@link Thread}, if any.
     */
    static void removeThreadLog() {
        JOB_LOG_HOLDER.remove()
    }

    static <T> T withThreadLog(Appendable out, Closure<T> code) {
        assert out != null
        assert code
        assert !threadLog

        threadLog = new AbstractSimpleLogger() {
            @Override
            protected void write(String buffer) {
                out.append(buffer)
                out.append('\n')
            }
        }
        try {
            return code()
        } finally {
            removeThreadLog()
        }
    }

    static <T> T withThreadLog(Logger logger, Closure<T> code) {
        assert logger
        assert code
        assert !threadLog

        threadLog = logger
        try {
            return code()
        } finally {
            removeThreadLog()
        }
    }
}

abstract class AbstractSimpleLogger extends MarkerIgnoringBase implements Logger {

    Level level = Level.INFO

    abstract protected void write(String buffer)

    @Override
    boolean isTraceEnabled() {
        return Level.TRACE.toInt() >= level.toInt()
    }

    @Override
    void trace(String msg) {
        if (isTraceEnabled()) {
            write(msg)
        }
    }

    @Override
    void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            write(MessageFormatter.format(format, arg).message)
        }
    }

    @Override
    void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            write(MessageFormatter.format(format, arg1, arg2).message)
        }
    }

    @Override
    void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            write(MessageFormatter.arrayFormat(format, arguments).message)
        }
    }

    @Override
    void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            write(MessageFormatter.format(msg, t).message)
        }
    }

    @Override
    boolean isDebugEnabled() {
        return Level.DEBUG.toInt() >= level.toInt()
    }

    @Override
    void debug(String msg) {
        if (isDebugEnabled()) {
            write(msg)
        }
    }

    @Override
    void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            write(MessageFormatter.format(format, arg).message)
        }
    }

    @Override
    void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            write(MessageFormatter.format(format, arg1, arg2).message)
        }
    }

    @Override
    void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            write(MessageFormatter.arrayFormat(format, arguments).message)
        }
    }

    @Override
    void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            write(MessageFormatter.format(msg, t).message)
        }
    }

    @Override
    boolean isInfoEnabled() {
        return Level.INFO.toInt() >= level.toInt()
    }

    @Override
    void info(String msg) {
        if (isInfoEnabled()) {
            write(msg)
        }
    }

    @Override
    void info(String format, Object arg) {
        if (isInfoEnabled()) {
            write(MessageFormatter.format(format, arg).message)
        }
    }

    @Override
    void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            write(MessageFormatter.format(format, arg1, arg2).message)
        }
    }

    @Override
    void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            write(MessageFormatter.arrayFormat(format, arguments).message)
        }
    }

    @Override
    void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            write(MessageFormatter.format(msg, t).message)
        }
    }

    @Override
    boolean isWarnEnabled() {
        return Level.WARN.toInt() >= level.toInt()
    }

    @Override
    void warn(String msg) {
        if (isWarnEnabled()) {
            write(msg)
        }
    }

    @Override
    void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            write(MessageFormatter.format(format, arg).message)
        }
    }

    @Override
    void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            write(MessageFormatter.format(format, arg1, arg2).message)
        }
    }

    @Override
    void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            write(MessageFormatter.arrayFormat(format, arguments).message)
        }
    }

    @Override
    void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            write(MessageFormatter.format(msg, t).message)
        }
    }

    @Override
    boolean isErrorEnabled() {
        return Level.ERROR.toInt() >= level.toInt()
    }

    @Override
    void error(String msg) {
        if (isErrorEnabled()) {
            write(msg)
        }
    }

    @Override
    void error(String format, Object arg) {
        if (isErrorEnabled()) {
            write(MessageFormatter.format(format, arg).message)
        }
    }

    @Override
    void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            write(MessageFormatter.format(format, arg1, arg2).message)
        }
    }

    @Override
    void error(String format, Object... arguments) {
        if (isErrorEnabled()) {
            write(MessageFormatter.arrayFormat(format, arguments).message)
        }
    }

    @Override
    void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            write(MessageFormatter.format(msg, t).message)
        }
    }
}
