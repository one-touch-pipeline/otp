package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

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
