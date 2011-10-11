package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple TestJob, just printing something to sysout.
 *
 */
@Component("testJob")
@Scope("prototype")
class TestJob extends AbstractJobImpl {
    @Override
    public void execute() throws Exception {
        println("Execute method of TestJob called")
    }
}
