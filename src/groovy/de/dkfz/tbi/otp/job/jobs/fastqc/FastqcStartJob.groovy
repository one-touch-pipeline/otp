package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.scheduling.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*
import org.springframework.beans.factory.annotation.*

@Component("fastqcStartJob")
@Scope("singleton")
class FastqcStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    SeqTrackService seqTrackService

    @Scheduled(fixedDelay=10000l)
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
