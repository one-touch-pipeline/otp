package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple Test implementation of the EndStateAware interface.
 * Does nothing useful.
 *
 */
@Component("testEndStateAwareJob")
@Scope("prototype")
class TestEndStateAwareJob extends AbstractEndStateAwareJobImpl {

    @Override
    public void execute() throws Exception {
        println("Execute method of TestEndStateAwareJob called")
        addOutputParameter("test", "1234")
        addOutputParameter("test2", "1234")
        addOutputParameter("test2", "4321")
        succeed()
    }

    @Override
    public String getVersion() {
        return ""
    }
}
