package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.SometimesResumableJob

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
