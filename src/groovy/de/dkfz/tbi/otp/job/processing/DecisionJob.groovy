package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.DecidingJobDefinition;
import de.dkfz.tbi.otp.job.plan.JobDecision

/**
 * Subinterface for Jobs which define a decision.
 * The Job has to select one possible outcome as a {@link JobDecision}.
 * The Job can query the available decisions through it's {@link ProcessingStep}.
 *
 * A DecisionJob is an {@link EndStateAwareJob} as it makes no sense to do a decision
 * without knowing whether it is correct. This means a DecisionJob has to succeed in order
 * to use the decision.
 * @see DecisionProcessingStep
 * @see DecidingJobDefinition
 */
interface DecisionJob extends EndStateAwareJob {
    /**
     * The Decision done by this Job.
     * @return The Decision
     * @throws InvalidStateException if the Job has not succeeded (yet).
     */
    public JobDecision getDecision() throws InvalidStateException
}
