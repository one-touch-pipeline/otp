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

import de.dkfz.tbi.otp.job.plan.JobDecision

/**
 * Abstract base class for {@link DecisionJob}s.
 *
 * @see DecisionJob
 */
@Deprecated
abstract class AbstractDecisionJobImpl extends AbstractEndStateAwareJobImpl implements DecisionJob {
    private JobDecision decision

    /**
     * Has to be called by an implementing Job to set the decision the
     * Job decided on. Automatically sets the Job as succeeded. If {@code decision}
     * is {@code null} the Job is set to failed.
     *
     * This method can be called multiple times.
     * @param decision The decision taken by the Job.
     * @see succeed
     * @see fail
     */
    protected final void setDecision(JobDecision decision) {
        if (decision) {
            succeed()
        } else {
            fail()
        }
        if (decision.jobDefinition != processingStep.jobDefinition) {
            throw new IncorrectProcessingException("Decision does not belong to current JobDefinition")
        }
        this.decision = decision
    }

    /**
     * @return List of available decisions the Job can take ordered by ID.
     */
    protected final List<JobDecision> getAvailableDecisions() {
        return JobDecision.findAllByJobDefinition(processingStep.jobDefinition).sort { it.id }
    }

    @Override
    final JobDecision getDecision() throws InvalidStateException {
        if (state != AbstractJobImpl.State.FINISHED && endState != ExecutionState.SUCCESS) {
            throw new InvalidStateException("Decision accessed, but not in succeeded state")
        }
        return decision
    }
}
