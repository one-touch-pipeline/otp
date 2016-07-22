package de.dkfz.tbi.otp.job.jobs.merging

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

@Component("mergingStartJob")
@Scope("singleton")
class MergingStartJob extends AbstractStartJobImpl {

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
    String getJobExecutionPlanName() {
        return "MergingWorkflow"
    }
}
