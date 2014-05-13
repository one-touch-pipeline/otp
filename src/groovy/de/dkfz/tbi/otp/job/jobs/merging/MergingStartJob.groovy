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
    MergingSetService mergingSetService

    @Autowired
    MergingPassService mergingPassService

    final int MAX_RUNNING = 4

    @Scheduled(fixedDelay = 60000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        MergingPass.withTransaction {
            MergingPass mergingPass = mergingPassService.create()
            if (mergingPass) {
                mergingPassService.mergingPassStarted(mergingPass)
                createProcess(new ProcessParameter(value: mergingPass.id.toString(), className: mergingPass.class.name))
                log.debug "MergingSetStartJob started for: ${mergingPass.toString()}"
            }
        }
    }

    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "MergingWorkflow", null, MAX_RUNNING)
        return (n < maxRunning)
    }
}
