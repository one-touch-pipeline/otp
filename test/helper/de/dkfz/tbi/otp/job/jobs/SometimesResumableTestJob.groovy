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
    public void execute() throws Exception {
        throw new Error('should never be called')
    }

    @Override
    public void planSuspend() {
        throw new Error('should never be called')
    }

    @Override
    public void cancelSuspend() {
        throw new Error('should never be called')
    }

    @Override
    public boolean isResumable() {
        return resumable
    }
}
