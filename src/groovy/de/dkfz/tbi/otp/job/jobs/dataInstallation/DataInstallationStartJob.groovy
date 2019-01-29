package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*

@Component("dataInstallationStartJob")
@Scope("singleton")
class DataInstallationStartJob extends AbstractStartJobImpl {

    @Autowired
    SeqTrackService seqTrackService

    @Scheduled(fixedDelay=5000L)
    @Override
    void execute() {
        doWithPersistenceInterceptor {
            ProcessingPriority minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority.priority > ProcessingPriority.MAXIMUM.priority) {
                return
            }

            SeqTrack seqTrack = seqTrackService.seqTrackReadyToInstall(minPriority)
            if (seqTrack) {
                SeqTrack.withTransaction {
                    trackingService.setStartedForSeqTracks([seqTrack], OtrsTicket.ProcessingStep.INSTALLATION)
                    seqTrack.dataInstallationState = SeqTrack.DataProcessingState.IN_PROGRESS
                    assert seqTrack.save(flush: true)
                    createProcess(seqTrack)
                    log.debug "Installing SeqTrack ${seqTrack} of run ${seqTrack.run.name}"
                }
            }
        }
    }
}
