package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.PbsJob

/**
 * Very simple FailingPbsTestJob, just printing something to sysout.
 * It is a failing Job as it does not return any PBS Ids.
 *
 */
class FailingPbsTestJob extends AbstractJobImpl implements PbsJob {
    @Override
    public void execute() throws Exception {
        println("Execute method of FailingPbsTestJob called")
    }

    @Override
    public List<String> getPbsIds() {
        return []
    }

    public Long getRealm() {
        return 1
    }
}
