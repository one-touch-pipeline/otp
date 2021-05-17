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

import de.dkfz.tbi.otp.job.plan.DecidingJobDefinition
import de.dkfz.tbi.otp.job.plan.JobDecision

/**
 * Subclass for a {@link ProcessingStep} which holds an outcome decision.
 * Only usable if the ProcessingStep's jobDefinition is a {@link DecidingJobDefinition}
 *
 * @see JobDecision
 * @see DecidingJobDefinition
 *
 * @deprecated class is part of the old workflow system
 */
@Deprecated
class DecisionProcessingStep extends ProcessingStep {
    /**
     * The decision produced by the Job. It is null as long as the Job has not finished.
     */
    JobDecision decision

    static constraints = {
        decision(nullable: true, validator: { JobDecision val, DecisionProcessingStep step ->
            if (!val) {
                return true
            }
            if (val.jobDefinition != step.jobDefinition) {
                return "jobDefinition"
            }
            return true
        })
    }
}
