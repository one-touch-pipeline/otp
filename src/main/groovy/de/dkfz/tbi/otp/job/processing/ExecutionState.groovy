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

enum ExecutionState {
    /** The Job has been created but not yet started*/
    CREATED,
    /** The Job has been started, that is the processing is running*/
    STARTED,
    /** The execution of the Job finished, but it is still unknown whether it succeeded or failed*/
    FINISHED,
    /** The execution of the Job finished successfully*/
    SUCCESS,
    /** The execution of the Job failed*/
    FAILURE,
    /** The Job has been restarted manually.
     *  The only allowed state before is FAILURE. No further update can follow after restarted. A new ProcessingStep is created.
     */
    RESTARTED,
    /** The execution of the Job has been suspended for a save shutdown. This state is only allowed after STARTED or RESUMED*/
    SUSPENDED,
    /** The execution of a suspended Job has been resumed. This state is only allowed after the state SUSPENDED.*/
    RESUMED
}
