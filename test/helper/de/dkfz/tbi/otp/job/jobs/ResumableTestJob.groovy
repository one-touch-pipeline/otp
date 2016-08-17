package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@ResumableJob
@UseJobLog
class ResumableTestJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        throw new Error('should never be called')
    }

}
