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
 * Interface for jobs that are resumable in certain phases of execution, but not in all.
 *
 * @see ResumableJob
 */
interface SometimesResumableJob extends Job {

    /**
     * Requests this job to go into a resumable state.
     *
     * <p>
     * This method must return as quickly as possible. It must <em>not</em> wait until the job
     * becomes resumable.
     */
    void planSuspend()

    /**
     * Allows this job to exit the resumable state.
     *
     * @see #isResumable()
     */
    void cancelSuspend()

    /**
     * Returns whether this job is currently in a resumable state.
     *
     * <p>
     * <strong>If {@link #planSuspend()} has been called before and this method returns
     * <code>true</code>, then this job must stay in the resumable state until
     * {@link #cancelSuspend()} is called.</strong>
     *
     * @return Whether this job is currently in a resumable state.
     */
    boolean isResumable()
}
