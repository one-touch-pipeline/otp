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
package de.dkfz.tbi.otp.job.processing

/**
 * Abstract base class for {@link ValidatingJob}s.
 *
 * @see ValidatingJob
 */
abstract class AbstractValidatingJobImpl extends AbstractEndStateAwareJobImpl implements ValidatingJob {
    private ProcessingStep validatedStep
    private Boolean validatedStepSucceeded = null

    /**
     * Implementing sub-classes can use this method to mark the validated job as succeeded or failed.
     * @param succeeded true for success, false for failure
     */
    protected void setValidatedSucceeded(boolean succeeded) {
        validatedStepSucceeded = succeeded
    }

    @Override
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    ProcessingStep getValidatorFor() {
        if (!validatedStep) {
            throw new RuntimeException("Validated Step accessed before set")
        }
        return validatedStep
    }

    @Override
    void setValidatorFor(ProcessingStep step) {
        validatedStep = step
    }

    @Override
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    boolean hasValidatedJobSucceeded() {
        if (validatedStepSucceeded == null) {
            throw new RuntimeException("Step not yet marked as validated")
        }
        return validatedStepSucceeded
    }
}
