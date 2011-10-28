package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDecision

/**
 * Subclass for a {@link ProcessingStep} which holds an outcome decision.
 * Only usable if the ProcessingStep's jobDefinition is a {@link DecidingJobDefinition}
 * @see JobDecision
 * @see DecidingJobDefinition
 */
class DecisionProcessingStep extends ProcessingStep {
    /**
     * The decision produced by the Job. It is null as long as the Job has not finished.
     */
    JobDecision decision

    static constraints = {
        decision(nullable: true)
    }
}
