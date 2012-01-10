package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

/**
 * Very simple test job which just throws an Exception during Execution.
 *
 */
class FailingTestJob extends AbstractJobImpl {
    @Override
    public void execute() throws Exception {
        throw new Exception("Testing")
    }
}
