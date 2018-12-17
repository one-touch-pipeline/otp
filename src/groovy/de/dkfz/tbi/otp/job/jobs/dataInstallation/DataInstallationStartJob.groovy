package de.dkfz.tbi.otp.job.jobs.dataInstallation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.tracking.OtrsTicket

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
