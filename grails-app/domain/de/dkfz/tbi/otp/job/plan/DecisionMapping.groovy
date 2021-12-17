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
package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.utils.Entity

/**
 * The DecisionMapping holds the mapping between {@link JobDecision}s
 * and {@link JobDefinition}s. It is used as a lookup table to find the
 * next to be executed JobDefinition based on the outcoming JobDecision of
 * a {@link DecisionJob}.
 *
 * @see JobDefinition
 * @see JobDecision
 * @see DecisionJob
 */
class DecisionMapping implements Entity {
    /**
     * The Decision which maps to...
     */
    JobDecision decision
    /**
     * ...a JobDefinition
     */
    JobDefinition definition

    static constraints = {
        decision(unique: true)
        definition(validator: { JobDefinition jobDefinition, DecisionMapping mapping ->
            if (!jobDefinition) {
                return false
            }
            if (!mapping.decision) {
                return false
            }
            if (jobDefinition == mapping.decision.jobDefinition) {
                return "recursive"
            }
            return (jobDefinition.plan == mapping.decision.jobDefinition.plan) ?: "plan"
        })
    }
}
