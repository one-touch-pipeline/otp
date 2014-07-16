package de.dkfz.tbi.otp.job.jobs.dataInstallation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.*

@Component("dataInstallationStartJob")
@Scope("singleton")
class DataInstallationStartJob extends AbstractStartJobImpl {

    @Autowired
    RunProcessingService runProcessingService

    @Autowired
    ProcessingOptionService optionService

    final int MAX_RUNNING = 2

    @Scheduled(fixedDelay=5000l)
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
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int numberOfRunning = numberOfRunningProcesses()
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "DataInstallationWorkflow", null, MAX_RUNNING)
        return (numberOfRunning < maxRunning)
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
        return "DataInstallationWorkflow"
    }
}
