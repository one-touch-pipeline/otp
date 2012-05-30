package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractValidatingJobImpl

/**
 * Very simple test job which sets the validated job to failure.
 *
 */
class FailingValidatingTestJob extends AbstractValidatingJobImpl {
    @Override
    public void execute() throws Exception {
        setValidatedSucceeded(false)
        succeed()
    }
}
