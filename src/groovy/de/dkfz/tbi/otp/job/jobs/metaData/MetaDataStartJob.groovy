package de.dkfz.tbi.otp.job.jobs.metaData

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.Run
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("metaDataStartJob")
@Scope("singleton")
class MetaDataStartJob extends AbstractStartJobImpl {

    final int MAX_RUNNING = 1

    @Scheduled(fixedRate=5000l)
    void execute() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            println("meta data Execution plan not set or not active")
            return
        }
        int numberOfRunning = numberOfRunningProcesses()
        if (numberOfRunning >= MAX_RUNNING) {
            println "MetaDataWorkflow: ${numberOfRunning} processes already running"
            return
        }
        int n=0
        List<Run> runs = Run.list()
        for(Run run in runs) {
            if (numberOfRunning >= MAX_RUNNING) {
                break
            }
            if (processed(run)) {
                continue
            }
            // new run to be processed
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
            println run.toString()
            numberOfRunning++
            n++
        }
        println "MetaDataWorkflow: ${n} jobs started"
    }

    /**
     * Checks if given run was already processed by MetaData execution plan
     * @param run
     * @return
     */
    private boolean processed(Run run) {
        List<ProcessParameter> processParameters =
            ProcessParameter.findAllByValue(run.id.toString(), run.class.name)
        for(ProcessParameter parameter in processParameters) {
            if (parameter.process.jobExecutionPlan.id == getExecutionPlan().id) {
                return true
            }
        }
    }

    /**
     * returns number of running processes for this execution plan
     * @return
     */
    private int numberOfRunningProcesses() {
        return Process.countByFinishedAndJobExecutionPlan(false, getExecutionPlan())
    }
}
