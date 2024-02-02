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
 * Interface for a Job which is allowed to update the state of a previously run Job.
 *
 * This interface can be used to verify that other Job(s) executed successfully or failed. The Job
 * should use its execute method to perform the check. It is possible that the Job execution takes
 * a long time as it has to be checked whether a long-running task finished. In such a case the Job
 * should ensure that it sleeps but never throw an exception due to threading. Remember throwing an
 * Exception will trigger that the Process fails and will require manual interaction.
 *
 * A validating job should never fail. It should always succeed. Because of that it is an
 * EndStateAwareJob. It knows that it succeeded when the end of the execute method is reached.
 */
interface ValidatingJob extends EndStateAwareJob {
    /**
     * @return ProcessingStep this Job is validating.
     */
    ProcessingStep getValidatorFor()

    /**
     * Used by the Scheduler to add a concrete Processing Step to validate
     * based on the information of the ValidatingJobDefinition.
     *
     * @param step A Processing Step this Job is validating
     */
    void setValidatorFor(ProcessingStep step)

    /**
     * Used by the Scheduler to determine whether the validated job succeeded
     * or failed.
     * In case the job failed the validated processing step is set to FAILURE and the Process is
     * marked as failed.
     * @return Whether the validated Job succeeded
     */
    boolean hasValidatedJobSucceeded()
}
