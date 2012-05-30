package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractValidatingJobImpl

/**
 * Very simple test job which sets the validated job to succeeded.
 *
 */
class ValidatingTestJob extends AbstractValidatingJobImpl {
    @Override
    public void execute() throws Exception {
        setValidatedSucceeded(true)
        succeed()
    }
}
