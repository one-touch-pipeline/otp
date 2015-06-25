package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl


class ParsePanCanQCJob extends AbstractEndStateAwareJobImpl {

    public void execute() {
        //TODO placeholder job for OTP-1449
        log.info('This job is only a placeholder for the implementation of OTP-1449 and does nothing')
        succeed()
    }
}
