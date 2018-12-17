package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ResumableJob

@Component
@Scope("prototype")
@ResumableJob
@UseJobLog
class ResumableTestJob extends AbstractJobImpl {

    @Override
    void execute() throws Exception {
        throw new Error('should never be called')
    }

}
