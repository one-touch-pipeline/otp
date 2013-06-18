package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("testPBSStartJob")
@Scope("singleton")
class TestPBSStartJob extends AbstractStartJobImpl  {

    final int MAX_RUNNING = 4

    @Autowired
    ProcessingOptionService optionService

    @Scheduled(fixedDelay=10000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        createProcess(new ProcessParameter(value: "test"))
    }

    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "TestPBSWorkflow", null, MAX_RUNNING)
        return (n < maxRunning)
    }
}
