package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

/**
 * Very simple Test implementation of the EndStateAware interface.
 * Does nothing useful.
 *
 */
@Component
@Scope("prototype")
@UseJobLog
class TestEndStateAwareJob extends AbstractEndStateAwareJobImpl {

    @Override
    public void execute() throws Exception {
        println("Execute method of TestEndStateAwareJob called")
        addOutputParameter("test", "1234")
        addOutputParameter("test2", "1234")
        addOutputParameter("test2", "4321")
        succeed()
    }
}
