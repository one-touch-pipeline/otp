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
        log.debug "start"
        if (!hasOpenSlot()) {
            log.debug "no open slots"
            return
        }
        log.debug "open slots"
        Run run = runProcessingService.runWithNewMetaData()
        log.debug "found run : ${run?.name}"
        if (run) {
            runProcessingService.blockMetaData(run)
            log.debug "run blocked"
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
            log.debug "MetaDataWorkflow started for: ${run.toString()}"
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
}
