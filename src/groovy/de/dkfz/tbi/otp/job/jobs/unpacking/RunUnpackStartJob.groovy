package de.dkfz.tbi.otp.job.jobs.unpacking

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.RunProcessingService;

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("runUnpackStartJob")
@Scope("singleton")
class RunUnpackStartJob extends AbstractStartJobImpl {

    @Autowired
    RunProcessingService runProcessingService

    final int MAX_RUNNING = 1

    @Scheduled(fixedDelay=2000l)
    void execute() {
        if (!hasOpenSlot()) {
            return
        }
        Run run = runProcessingService.runReadyToUnpack()
        if (run) {
            runProcessingService.blockUnpack(run)
            createProcess(run)
        }
    }

    boolean hasOpenSlot() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            return false
        }
        int numberOfRunning = numberOfRunningProcesses()
        if (numberOfRunning >= MAX_RUNNING) {
            return false
        }
        return true
    }

    /**
     * returns number of running processes for this execution plan
     * @return
     */
    private int numberOfRunningProcesses() {
        return Process.countByFinishedAndJobExecutionPlan(false, getExecutionPlan())
    }

    @Override
    protected String getJobExecutionPlanName() {
        return "runUnpack"
    }
}
