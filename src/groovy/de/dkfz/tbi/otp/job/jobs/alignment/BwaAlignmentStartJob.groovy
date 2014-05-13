package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
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
        if (!hasFreeSlot()) {
            return
        }
        AlignmentPass.withTransaction {
            AlignmentPass alignmentPass = alignmentPassService.createAlignmentPass()
            if (alignmentPass) {
                log.debug "Creating Alignment process for AlignmentPass ${alignmentPass}"
                alignmentPassService.alignmentPassStarted(alignmentPass)
                createProcess(new ProcessParameter(value: alignmentPass.id.toString(), className: alignmentPass.class.name))
            }
        }
    }

    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "BwaAlignmentWorkflow", null, MAX_RUNNING)
        return (n < maxRunning)
    }
}