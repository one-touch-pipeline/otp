package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

/**
 * Very simple test job which sets the validated job to succeeded.
 *
 */
@Component
@Scope("prototype")
@UseJobLog
class ValidatingTestJob extends AbstractValidatingJobImpl {
    @Override
    public void execute() throws Exception {
        setValidatedSucceeded(true)
        succeed()
    }
}
