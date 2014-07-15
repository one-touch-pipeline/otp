package de.dkfz.tbi.otp.job.jobs.metaData

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("metaDataStartJob")
@Scope("singleton")
class MetaDataStartJob extends AbstractStartJobImpl {

    @Autowired
    RunProcessingService runProcessingService

    final int MAX_RUNNING = 1

    @Scheduled(fixedDelay=5000l)
    void execute() {
        if (!hasOpenSlot()) {
            return
        }
        Run run = runProcessingService.runWithNewMetaData()
        if (run) {
            runProcessingService.blockMetaData(run)
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
            println "MetaDataWorkflow started for: ${run.toString()}"
        }
    }

    boolean hasOpenSlot() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            //println("meta data Execution plan not set or not active")
            return false
        }
        int numberOfRunning = numberOfRunningProcesses()
        if (numberOfRunning >= MAX_RUNNING) {
            //println "MetaDataWorkflow: ${numberOfRunning} processes already running"
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
        return "loadMetaData"
    }
}
