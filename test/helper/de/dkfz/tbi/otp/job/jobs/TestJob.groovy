package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

/**
 * Very simple TestJob, just printing something to sysout.
 */
@Component
@Scope("prototype")
@UseJobLog
class TestJob extends AbstractJobImpl {
    @Override
    void execute() throws Exception {
        println("Execute method of TestJob called")
        addOutputParameter("test", "1234")
        addOutputParameter("test2", "1234")
        addOutputParameter("test2", "4321")
    }
}
