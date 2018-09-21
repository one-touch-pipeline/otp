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

    @Scheduled(fixedDelay=5000l)
    @Override
    void execute() {
        doWithPersistenceInterceptor {
            short minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
                return
            }

            List<SeqTrack> seqTracks = seqTrackService.seqTracksReadyToInstall(minPriority)
            if (seqTracks) {
                for (SeqTrack seqTrack : seqTracks) {
                    if (seqTrack.processingPriority >= minimumProcessingPriorityForOccupyingASlot) {
                        SeqTrack.withTransaction {
                            trackingService.setStartedForSeqTracks([seqTrack], OtrsTicket.ProcessingStep.INSTALLATION)
                            seqTrack.dataInstallationState = SeqTrack.DataProcessingState.IN_PROGRESS
                            assert seqTrack.save(flush: true)
                            createProcess(seqTrack)
                            log.debug "Installing SeqTrack ${seqTrack} of run ${seqTrack.run.name}"
                        }
                    } else {
                        break
                    }
                }
            }
        }
    }
}
