package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

/**
 * Very simple test job which sets the validated job to failure.
 */
@Component
@Scope("prototype")
@UseJobLog
class FailingValidatingTestJob extends AbstractValidatingJobImpl {
    @Override
    void execute() throws Exception {
        setValidatedSucceeded(false)
        succeed()
    }
}
