package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.EndStateAwareJob

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple Test implementation of the EndStateAware interface which fails.
 *
 */
@Component("testFailureEndStateAwareJob")
@Scope("prototype")
class FailingTestEndStateAwareJob extends AbstractEndStateAwareJobImpl {

    @Override
    public void execute() throws Exception {
        println("Execute method of FailingTestEndStateAwareJob called")
        addOutputParameter("test", "1234")
        addOutputParameter("test2", "1234")
        addOutputParameter("test2", "4321")
        fail()
    }
}
