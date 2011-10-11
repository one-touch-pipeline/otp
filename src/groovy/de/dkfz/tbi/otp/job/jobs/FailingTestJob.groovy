package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple test job which just throws an Exception during Execution.
 *
 */
@Component("failingTestJob")
@Scope("prototype")
class FailingTestJob extends AbstractJobImpl {
    @Override
    public void execute() throws Exception {
        throw new Exception("Testing")
    }
}
