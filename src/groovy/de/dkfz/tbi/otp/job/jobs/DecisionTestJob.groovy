package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractDecisionJobImpl

/**
 * Test job for a decision, taking the first available decision as it's outcome.
 *
 */
class DecisionTestJob extends AbstractDecisionJobImpl {

    @Override
    public void execute() throws Exception {
        println("Executing Decision Test Job")
        setDecision(getAvailableDecisions().first())
    }

}
