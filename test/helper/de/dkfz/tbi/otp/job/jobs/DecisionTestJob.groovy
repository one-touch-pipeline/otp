package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractDecisionJobImpl

/**
 * Test job for a decision, taking the first available decision as it's outcome.
 */
@Component
@Scope("prototype")
@UseJobLog
class DecisionTestJob extends AbstractDecisionJobImpl {

    @Override
    void execute() throws Exception {
        println("Executing Decision Test Job")
        setDecision(getAvailableDecisions().first())
    }

}
