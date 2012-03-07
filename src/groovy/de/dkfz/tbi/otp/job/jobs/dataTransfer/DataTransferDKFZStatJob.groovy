package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.Run
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("dataTransferDKFZStartJob")
@Scope("singleton")
class DataTransferDKFZStatJob extends AbstractStartJobImpl  {

    final int MAX_RUNNING = 4

    @Scheduled(fixedRate=60000l)
    void execute() {
        if (!hasSlot()) {
            return
        }
        int n = 0;
        int nRunning = numberOfRunningProcesses()
        List<Run> runs = Run.findAllByCompleteAndRealm(true, Run.Realm.DKFZ)
        for(Run run in runs) {
            if (!hasSlot()) {
                break
            }
            if (nRunning >= MAX_RUNNING) {
                break
            }
            if (processed(run)) {
                continue
            }
            // new run to be processed
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
            println run.toString()
            nRunning++
            n++
        }
        println "DataTransferWorkflow: ${n} jobs started"
    }

    private boolean hasSlot() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            //println("transfer Execution plan not set or not active")
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
    * Checks if given run was already processed by MetaData execution plan
    * @param run
    * @return
    */
   boolean processed(Run run) {
       List<ProcessParameter> processParameters =
           ProcessParameter.findAllByValue(run.id.toString()) //, run.class.name)
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