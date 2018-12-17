package de.dkfz.tbi.otp.job.jobs

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.tracking.TrackingService

@Component
@Scope("prototype")
@UseJobLog
class NotifyProcessFinishedJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    TrackingService trackingService

    @Override
    void execute() throws Exception {
        log.trace("NotifyProcessFinishedJob: entering trackingService.processFinished")
        trackingService.processFinished(processParameterObject.containedSeqTracks)
        log.trace("NotifyProcessFinishedJob: entering succeed")
        succeed()
        log.trace("NotifyProcessFinishedJob: succeeded")
    }
}
