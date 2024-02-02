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
package de.dkfz.tbi.otp.job.processing

/**
 * This interface can be used for Jobs which are able to decide whether they succeeded or failed.
 *
 * Not all Jobs are able to decide whether their operation succeeded or failed. For Jobs which are
 * able to decide whether they succeeded or not this interface can be used instead of the more
 * generic Job interface.
 *
 * The interface can be used by the framework to directly determine whether the job succeeded.
 * This interface should only be used by short jobs. A job should not perform own error handling
 * in order to determine whether it succeeded. Such tasks are better passed to a {@link ValidatingJob}.
 */
interface EndStateAwareJob extends Job {
    /**
     * @return The ExecutionState after the method finished as determined by the Job itself.
     * @throws InvalidStateException If the Job execution has not yet finished.
     */
    ExecutionState getEndState() throws InvalidStateException
}
