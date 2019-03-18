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

package de.dkfz.tbi.otp.utils

import org.apache.commons.logging.Log
import org.junit.Assert

/**
 * A logger to check log statement.
 *
 * The idea behind this logger is the check of occured log messages against expected ones.
 *
 * The developer defined the expected message(s) on the expected level with one of the add methods,
 * for example {@link #addError(Object)} for an error message.
 *
 * When a message is send to the logger, the reveived message inclusive the level is checked with the next expected
 * message on the expected level. If they do not match an assertion is thrown. Also an exception is thrown if no further
 * message is defined.
 *
 * With the method {@link #assertAllMessagesConsumed()} it is possible to check that all expected messages were checked
 * against an incomming message.
 *
 * If an exception is thrown, the further checks are disabled, because the queue may no longer be correct.
 */
class CheckedLogger implements Log {

    /**
     * Defines the message level.
     */
    enum Level {
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
    }

    /**
     * Defines an expected message to use for checks
     */
    //@Immutable doesn't work, since messsage is of type object, which is not immutable
    private class ExpectedMessage {

        final Level level

        final Object message

        ExpectedMessage(Level level, Object message) {
            super()
            this.level = level
            this.message = message
        }

        @Override
        String toString() {
            "${level}: ${message}"
        }
    }



    /**
     * Holds th remaing expected messages.
     */
    private Queue<ExpectedMessage> expectedMessages = [] as Queue

    /**
     * Flag to indicate that an exception was already thrown. In that case no further messages should be processed,
     * because the order or the messages itself could be changed by that event.
     * Also the check {@link #assertAllMessagesConsumed()} is then disabled.
     */
    private boolean hasAlreadyThrownAnException = false



    /**
     * Add a message expected on the fatal level
     */
    CheckedLogger addFatal(Object message) {
        expectedMessages << new ExpectedMessage(Level.FATAL, message)
        return this
    }

    /**
     * Add a message expected on the error level
     */
    CheckedLogger addError(Object message) {
        expectedMessages << new ExpectedMessage(Level.ERROR, message)
        return this
    }

    /**
     * Add a message expected on the warn level
     */
    CheckedLogger addWarning(Object message) {
        expectedMessages << new ExpectedMessage(Level.WARN, message)
        return this
    }

    /**
     * Add a message expected on the info level
     */
    CheckedLogger addInfo(Object message) {
        expectedMessages << new ExpectedMessage(Level.INFO, message)
        return this
    }

    /**
     * Add a message expected on the debug level
     */
    CheckedLogger addDebug(Object message) {
        expectedMessages << new ExpectedMessage(Level.DEBUG, message)
        return this
    }

    /**
     * Add a message expected on the trace level
     */
    CheckedLogger addTrace(Object message) {
        expectedMessages << new ExpectedMessage(Level.TRACE, message)
        return this
    }



    /**
     * ensures that all expected messages were used for checks.
     * If a check has thrown an exception, no further check is done.
     */
    void assertAllMessagesConsumed() {
        if (hasAlreadyThrownAnException) {
            return
        }
        assert expectedMessages.empty
    }



    private void checkConsumedMessage(Level level, Object message) {
        if (hasAlreadyThrownAnException) {
            return
        }

        if (expectedMessages.empty) {
            Assert.fail("An unexpected message was send to logger: ${level} ${message}")
        }
        ExpectedMessage expectedMessage = expectedMessages.poll()
        if (expectedMessage.level != level) {
            if (expectedMessage.message == message) {
                hasAlreadyThrownAnException = true
                Assert.fail("Message was send on wrong level to logger. Eexpected: ${expectedMessage.level} but got ${level}\nthe message: ${message}")
            } else {
                hasAlreadyThrownAnException = true
                Assert.fail("An unexpected message was send to logger:\nExpected: ${expectedMessage.level} ${expectedMessage.message}\nbut got: ${level} ${message}")
            }
        } else {
            if (expectedMessage.message != message) {
                hasAlreadyThrownAnException = true
                Assert.fail("Wrong message was send on corect level ($level}:\nExpected message: ${expectedMessage.message}\nReceived Message: ${message}")
            }
        }
    }



    @Override
    void fatal(Object object) {
        checkConsumedMessage(Level.FATAL, object)
    }

    @Override
    void fatal(Object object, Throwable throwable) {
        checkConsumedMessage(Level.FATAL, object)
    }

    @Override
    void error(Object object) {
        checkConsumedMessage(Level.ERROR, object)
    }

    @Override
    void error(Object object, Throwable throwable) {
        checkConsumedMessage(Level.ERROR, object)
    }

    @Override
    void warn(Object object) {
        checkConsumedMessage(Level.WARN, object)
    }

    @Override
    void warn(Object object, Throwable throwable) {
        checkConsumedMessage(Level.WARN, object)
    }

    @Override
    void debug(Object object) {
        checkConsumedMessage(Level.DEBUG, object)
    }

    @Override
    void debug(Object object, Throwable throwable) {
        checkConsumedMessage(Level.DEBUG, object)
    }

    @Override
    void info(Object object) {
        checkConsumedMessage(Level.INFO, object)
    }

    @Override
    void info(Object object, Throwable throwable) {
        checkConsumedMessage(Level.INFO, object)
    }

    @Override
    void trace(Object object) {
        checkConsumedMessage(Level.TRACE, object)
    }

    @Override
    void trace(Object object, Throwable throwable) {
        checkConsumedMessage(Level.TRACE, object)
    }



    @Override
    boolean isFatalEnabled() {
        Assert.fail("Method isFatalEnabled not expected to be called")
    }

    @Override
    boolean isErrorEnabled() {
        Assert.fail("Method isErrorEnabled not expected to be called")
    }

    @Override
    boolean isWarnEnabled() {
        Assert.fail("Method isWarnEnabled not expected to be called")
    }

    @Override
    boolean isInfoEnabled() {
        Assert.fail("Method isInfoEnabled not expected to be called")
    }

    @Override
    boolean isDebugEnabled() {
        Assert.fail("Method isDebugEnabled not expected to be called")
    }

    @Override
    boolean isTraceEnabled() {
        Assert.fail("Method isTraceEnabled not expected to be called")
    }
}
