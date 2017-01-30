package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*

@Component("mergingStartJob")
@Scope("singleton")
class MergingStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    MergingPassService mergingPassService

    final int MAX_RUNNING = 4

    @Scheduled(fixedDelay = 60000l)
    void execute() {

        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }

        MergingPass.withTransaction {
            MergingPass mergingPass = mergingPassService.create(minPriority)
            if (mergingPass) {
                mergingPassService.mergingPassStarted(mergingPass)
                createProcess(mergingPass)
                log.debug "MergingStartJob started for: ${mergingPass.toString()}"
            }
        }
    }

    @Override
    Process restart(Process process) {
        assert process

        MergingPass failedInstance = (MergingPass)process.getProcessParameterObject()

        MergingPass.withTransaction {
            CollectionUtils.atMostOneElement(ProcessedMergedBamFile.findAllByMergingPass(failedInstance))?.withdraw()
            MergingPass newMergingPass = new MergingPass(
                    mergingSet: failedInstance.mergingSet,
                    identifier: MergingPass.nextIdentifier(failedInstance.mergingSet),
            ).save(flush: true)
            mergingPassService.mergingPassStarted(newMergingPass)

            return createProcess(newMergingPass)
        }
    }
}
