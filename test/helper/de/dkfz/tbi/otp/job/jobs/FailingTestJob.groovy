package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.utils.HelperUtils

/**
 * Very simple test job which just throws an Exception during Execution.
 */
@Component
@Scope("prototype")
@UseJobLog
class FailingTestJob extends AbstractJobImpl implements AutoRestartableJob {

    static final String EXCEPTION_MESSAGE = HelperUtils.uniqueString

    @Override
    void execute() throws Exception {
        throw new Exception(EXCEPTION_MESSAGE)
    }
}
