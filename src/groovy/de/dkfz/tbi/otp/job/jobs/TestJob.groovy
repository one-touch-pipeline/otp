package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

/**
 * Very simple TestJob, just printing something to sysout.
 *
 */
class TestJob extends AbstractJobImpl {
    @Override
    public void execute() throws Exception {
        println("Execute method of TestJob called")
        addOutputParameter("test", "1234")
        addOutputParameter("test2", "1234")
        addOutputParameter("test2", "4321")
    }
}
