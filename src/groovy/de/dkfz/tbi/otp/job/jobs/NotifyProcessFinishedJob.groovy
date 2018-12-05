package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class NotifyProcessFinishedJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {

    @Autowired
    TrackingService trackingService

    @Override
    void execute() throws Exception {
        log.debug("try: processFinished")
        trackingService.processFinished(processParameterObject.containedSeqTracks)
        log.debug("try: succeed")
        succeed()
        log.debug("succeeded")
    }
}
