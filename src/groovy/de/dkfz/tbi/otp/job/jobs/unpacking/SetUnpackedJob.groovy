package de.dkfz.tbi.otp.job.jobs.unpacking

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.Autowired

class SetUnpackedJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    RunProcessingService runProcessingService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        runProcessingService.setUnpackComplete(run)
        succeed()
    }
}
