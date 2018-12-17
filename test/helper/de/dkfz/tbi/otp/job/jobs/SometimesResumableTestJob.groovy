package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.SometimesResumableJob

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
