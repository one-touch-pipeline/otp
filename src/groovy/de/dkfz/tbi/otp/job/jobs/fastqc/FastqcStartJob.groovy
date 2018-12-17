package de.dkfz.tbi.otp.job.jobs.fastqc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.tracking.OtrsTicket

@Component("fastqcStartJob")
@Scope("singleton")
class FastqcStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    SeqTrackService seqTrackService

    @Scheduled(fixedDelay=10000L)
    @Override
    void execute() {
        doWithPersistenceInterceptor {
            ProcessingPriority minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority.priority > ProcessingPriority.MAXIMUM.priority) {
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
    }

    @Override
    Process restart(Process process) {
        assert process

        SeqTrack seqTrack = (SeqTrack)process.getProcessParameterObject()

        SeqTrack.withTransaction {
            SeqTrackService.setFastqcInProgress(seqTrack)
            FastqcProcessedFile.withCriteria {
                dataFile {
                    eq "seqTrack", seqTrack
                }
            }*.delete()
            return createProcess(seqTrack)
        }
    }
}
