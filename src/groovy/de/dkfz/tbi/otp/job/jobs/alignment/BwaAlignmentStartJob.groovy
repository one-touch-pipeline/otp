package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("BwaAlignmentStartJob")
@Scope("singleton")
class BwaAlignmentStartJob extends AbstractStartJobImpl {

    @Autowired
    AlignmentPassService alignmentPassService

    @Autowired
    ProcessingOptionService optionService

    final int MAX_RUNNING = 4

    @Scheduled(fixedDelay=10000l)
    void execute() {
        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }
        AlignmentPass.withTransaction {
            AlignmentPass alignmentPass = alignmentPassService.findAlignmentPassForProcessing(minPriority)
            if (alignmentPass) {
                log.debug "Creating Alignment process for AlignmentPass ${alignmentPass}"
                alignmentPassService.alignmentPassStarted(alignmentPass)
                createProcess(alignmentPass)
            }
        }
    }

    @Override
    protected String getJobExecutionPlanName() {
        return "ConveyBwaAlignmentWorkflow"
    }
}
