package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl

/**
 * Very simple Test implementation of the EndStateAware interface.
 * Does nothing useful.
 */
@Component
@Scope("prototype")
@UseJobLog
class TestEndStateAwareJob extends AbstractEndStateAwareJobImpl {

    @Override
    void execute() throws Exception {
        println("Execute method of TestEndStateAwareJob called")
        addOutputParameter("test", "1234")
        addOutputParameter("test2", "1234")
        addOutputParameter("test2", "4321")
        succeed()
    }
}
