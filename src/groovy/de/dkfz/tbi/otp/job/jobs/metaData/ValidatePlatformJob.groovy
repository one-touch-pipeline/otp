package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.SeqPlatformService

class ValidatePlatformJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    SeqPlatformService seqPlatformService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        if (seqPlatformService.validateSeqPlatform(runId)) {
            succeed()
        } else {
            fail()
        }
    }
}
