package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

/**
 * Test job for a decision, taking the first available decision as it's outcome.
 *
 */
@Component
@Scope("prototype")
@UseJobLog
class DecisionTestJob extends AbstractDecisionJobImpl {

    @Override
    public void execute() throws Exception {
        println("Executing Decision Test Job")
        setDecision(getAvailableDecisions().first())
    }

}
