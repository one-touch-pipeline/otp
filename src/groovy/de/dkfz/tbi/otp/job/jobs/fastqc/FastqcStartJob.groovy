package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("fastqcStartJob")
@Scope("singleton")
class FastqcStartJob extends AbstractStartJobImpl {

    @Autowired
    SeqTrackService seqTrackService

    @Scheduled(fixedDelay=10000l)
    void execute() {
        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }

        SeqTrack.withTransaction {
            SeqTrack seqTrack = seqTrackService.getSeqTrackReadyForFastqcProcessing(minPriority)
            if (seqTrack) {
                trackingService.setStartedForSeqTracks(seqTrack.containedSeqTracks, OtrsTicket.ProcessingStep.FASTQC)
                log.debug "Creating fastqc process for seqTrack ${seqTrack}"
                seqTrackService.setFastqcInProgress(seqTrack)
                createProcess(seqTrack)
            }
        }
    }

    @Override
    String getJobExecutionPlanName() {
        return "FastqcWorkflow"
    }
}
