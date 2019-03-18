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

import de.dkfz.tbi.otp.job.processing.ProcessingStep

/**
 * Simple wrapper around a message which needs to be logged together
 * with a ProcessingStep.
 *
 * This class is needed to log messages in the Jobs. As the log output
 * of each Job should go into an independent file and the logging should
 * be as convenient to the developers as possible the {@link JobLog} takes
 * care of logging instances of this class to wrap the actual message.
 *
 * The Appender can take care about adjusting the actual logging to the
 * resource by taking the ProcessingStep into account.
 */
class JobLogMessage {

    /**
     * The original message logged by the Job.
     */
    Object message
    /**
     * The ProcessingStep for which the message got logged.
     */
    ProcessingStep processingStep

    JobLogMessage(Object message, ProcessingStep processingStep) {
        this.message = message
        this.processingStep = processingStep
    }

    @Override
    String toString() {
        return "[ProcessingStep ${processingStep.id}] ${message}"
    }
}
