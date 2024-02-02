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

import de.dkfz.tbi.otp.job.plan.DecidingJobDefinition
import de.dkfz.tbi.otp.job.plan.JobDecision

/**
 * Subinterface for Jobs which define a decision.
 * The Job has to select one possible outcome as a {@link JobDecision}.
 * The Job can query the available decisions through it's {@link ProcessingStep}.
 *
 * A DecisionJob is an {@link EndStateAwareJob} as it makes no sense to do a decision
 * without knowing whether it is correct. This means a DecisionJob has to succeed in order
 * to use the decision.
 *
 * @see DecisionProcessingStep
 * @see DecidingJobDefinition
 */
interface DecisionJob extends EndStateAwareJob {
    /**
     * The Decision done by this Job.
     * @return The Decision
     * @throws InvalidStateException if the Job has not succeeded (yet).
     */
    JobDecision getDecision() throws InvalidStateException
}
