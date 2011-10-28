package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.plan.DecidingJobDefinition
import de.dkfz.tbi.otp.job.plan.JobDecision
import de.dkfz.tbi.otp.job.plan.DecisionMapping
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.DecisionJob
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.InvalidStateException

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Test job for a decision, taking the first available decision as it's outcome.
 *
 */
@Component("decisionTestJob")
@Scope("prototype")
class DecisionTestJob extends AbstractJobImpl implements DecisionJob {

    @Override
    public ExecutionState getEndState() throws InvalidStateException {
        return ExecutionState.SUCCESS
    }

    @Override
    public void execute() throws Exception {
        println("Executing Decision Test Job")
    }

    @Override
    public JobDecision getDecision() throws InvalidStateException {
        return DecisionMapping.findAllByDecisionInList(JobDecision.findAllByJobDefinition(getProcessingStep().jobDefinition)).first().decision
    }

}
