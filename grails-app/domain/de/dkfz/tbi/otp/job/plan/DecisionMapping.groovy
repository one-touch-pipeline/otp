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
            if (jobDefinition.plan != mapping.decision.jobDefinition.plan) {
                return "plan"
            }
            return true
        })
    }
}
