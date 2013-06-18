package de.dkfz.tbi.otp.job.jobs.dataLocation

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.RunProcessingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
@Component("dataLocationStartJob")
@Scope("singleton")
class DataLocationStartJob extends AbstractStartJobImpl {

    @Autowired
    RunProcessingService runProcessingService

    final int MAX_RUNNING = 1

    @Scheduled(fixedDelay=3000l)
    void execute() {
        if (!hasOpenSlots()) {
            return
        }
        Run run = runProcessingService.runReadyToCheckFinalLocation()
        if (run) {
            println "RUN: ${run.id}"
            runProcessingService.blockCheckingFinalLocation(run)
            println "BLOCKED RUN: ${run.id}"
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
            println "Starting dataLocationWorkflow for run ${run.name}"
        }
    }

    boolean hasOpenSlots() {
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
}