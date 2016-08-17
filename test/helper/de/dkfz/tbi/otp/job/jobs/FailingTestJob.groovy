package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

/**
 * Very simple test job which just throws an Exception during Execution.
 *
 */
@Component
@Scope("prototype")
@UseJobLog
class FailingTestJob extends AbstractJobImpl implements AutoRestartableJob {

    static final String EXCEPTION_MESSAGE = HelperUtils.uniqueString

    @Override
    public void execute() throws Exception {
        throw new Exception(EXCEPTION_MESSAGE)
    }
}
