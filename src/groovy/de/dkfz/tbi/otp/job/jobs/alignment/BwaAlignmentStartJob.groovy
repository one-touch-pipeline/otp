package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*


@Component("BwaAlignmentStartJob")
@Scope("singleton")
class BwaAlignmentStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    AlignmentPassService alignmentPassService

    @Scheduled(fixedDelay=10000l)
    void execute() {
        doWithPersistenceInterceptor {
            short minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
                return
            }
            AlignmentPass.withTransaction {
                AlignmentPass alignmentPass = alignmentPassService.findAlignmentPassForProcessing(minPriority)
                if (alignmentPass) {
                    trackingService.setStartedForSeqTracks(alignmentPass.containedSeqTracks, OtrsTicket.ProcessingStep.ALIGNMENT)
                    log.debug "Creating Alignment process for AlignmentPass ${alignmentPass}"
                    alignmentPassService.alignmentPassStarted(alignmentPass)
                    createProcess(alignmentPass)
                }
            }
        }
    }

    @Override
    Process restart(Process process) {
        assert process

        AlignmentPass failedInstance = (AlignmentPass)(process.getProcessParameterObject())

        AlignmentPass.withTransaction {
            AlignmentPass newInstance = new AlignmentPass(
                workPackage: failedInstance.workPackage,
                seqTrack: failedInstance.seqTrack,
                identifier: AlignmentPass.nextIdentifier(failedInstance.seqTrack),
                alignmentState: AlignmentPass.AlignmentState.IN_PROGRESS,
            )
            assert newInstance.save(flush: true)
            return createProcess(newInstance)
        }
    }
}
