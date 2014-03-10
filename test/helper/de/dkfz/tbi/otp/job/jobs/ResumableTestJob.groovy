package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ResumableJob

@ResumableJob
class ResumableTestJob extends AbstractJobImpl {

    @Override
    public void execute() throws Exception {
        throw new Error('should never be called')
    }

}
