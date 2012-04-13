package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.Run
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("dataInstallationStartJob")
@Scope("singleton")
class DataInstallationStartJob extends AbstractStartJobImpl {

    final int MAX_RUNNING = 1

    @Scheduled(fixedRate=5000l)
    void execute() {
        if (!hasOpenSlots()) {
            return
        }
        int n = 0;
        List<Run> runs = listOfRuns()
        for(Run run in runs) {
            if (!hasOpenSlots()) {
                break
            }
            if (processed(run)) {
                continue
            }
            // new run to be processed
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
            println run.toString()
            n++
        }
        if (n>0) {
            println "DataInstallationWorkflow: ${n} jobs started"
        }
    }

    List<Run> listOfRuns() {
        def c = Run.createCriteria()
        List<Run> runs = c.list {
            and{
                eq("complete", true)
                eq("finalLocation", false)
                eq("legacyRun", false)
            }
        }
    }

    /**
    * Checks if given run was already processed by DataLocation execution plan
    * @param run
    * @return
    */
    boolean processed(Run run) {
        List<ProcessParameter> processParameters =
            ProcessParameter.findAllByValue(run.id.toString(), run.class.name)
        for(ProcessParameter parameter in processParameters) {
            if (parameter.process.jobExecutionPlan.id == getExecutionPlan().id) {
                return true
            }
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