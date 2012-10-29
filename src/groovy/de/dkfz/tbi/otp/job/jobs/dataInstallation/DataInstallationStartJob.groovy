package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("dataInstallationStartJob")
@Scope("singleton")
class DataInstallationStartJob extends AbstractStartJobImpl {

    @Autowired
    RunProcessingService runProcessingService

    final int MAX_RUNNING = 2

    @Scheduled(fixedRate=5000l)
    void execute() {
        if (!hasOpenSlots()) {
            return
        }
        Run run = runProcessingService.runReadyToInstall()
        if (run) {
            runProcessingService.blockInstallation(run)
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
            log.debug "Installing Run: ${run.name}"
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