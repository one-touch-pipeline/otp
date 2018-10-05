package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class SometimesResumableTestJob extends AbstractJobImpl implements SometimesResumableJob {

    boolean resumable

    @Override
    void execute() throws Exception {
        throw new Error('should never be called')
    }

    @Override
    void planSuspend() {
        throw new Error('should never be called')
    }

    @Override
    void cancelSuspend() {
        throw new Error('should never be called')
    }

    @Override
    boolean isResumable() {
        return resumable
    }
}
