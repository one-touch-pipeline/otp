package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractDecisionJobImpl

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Test job for a decision, taking the first available decision as it's outcome.
 *
 */
@Component("decisionTestJob")
@Scope("prototype")
class DecisionTestJob extends AbstractDecisionJobImpl {

    @Override
    public void execute() throws Exception {
        println("Executing Decision Test Job")
        setDecision(getAvailableDecisions().first())
    }

}
