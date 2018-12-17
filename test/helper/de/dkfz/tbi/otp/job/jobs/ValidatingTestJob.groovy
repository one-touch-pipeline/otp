package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractValidatingJobImpl

/**
 * Very simple test job which sets the validated job to succeeded.
 */
@Component
@Scope("prototype")
@UseJobLog
class ValidatingTestJob extends AbstractValidatingJobImpl {
    @Override
    void execute() throws Exception {
        setValidatedSucceeded(true)
        succeed()
    }
}
