package de.dkfz.tbi.otp.job.jobs.merging

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*


class MergingStartJob extends AbstractStartJobImpl {

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    MergingSetService mergingSetService

    @Autowired
    MergingPassService mergingPassService

    final int MAX_RUNNING = 1

    @Scheduled(fixedRate=60000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        MergingPass mergingPass = mergingPassService.create()
        if (mergingPass) {
            mergingSetService.blockForMerging(mergingPass.mergingSet)
            createProcess(new ProcessParameter(value: mergingPass.id.toString(), className: MergingPass.class.name))
            log.debug "MergingSetStartJob started for: ${mergingPass.toString()}"
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