package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*

class NotifyProcessFinishedJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    TrackingService trackingService

    @Override
    public void execute() throws Exception {
        String step = getParameterValueOrClass("step")
        trackingService.processFinished(processParameterObject.containedSeqTracks, OtrsTicket.ProcessingStep.valueOf(step))
        succeed()
    }
}
