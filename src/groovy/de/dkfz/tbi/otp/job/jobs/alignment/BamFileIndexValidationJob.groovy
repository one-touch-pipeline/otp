package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.job.processing.*

class BamFileIndexValidationJob  extends AbstractEndStateAwareJobImpl {

    @Override
    public void execute() throws Exception {
        /*
         * The index file for single lane BAM files is no longer needed, therefore it is not needed to check for the index file anymore.
         * TODO: Remove this entire job after OTP-505 or during OTP-1165
         */
        succeed()
    }
}
