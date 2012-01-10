package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.Run
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("dataTransferStartJob")
@Scope("singleton")
class DataTransferStatJob extends AbstractStartJobImpl  {
    boolean performed = true

    int N = 1

    @Scheduled(fixedRate=60000l)
    void execute() {       
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            println("transfer Execution plan not set or not active")
            return
        }
        println "Executing Data Transfer workflow with N = " + N
        if (N > 10) {
            return
        }
        if (performed) {
            println "Jobs already started ..."
            return
        }
        //return
        println("Transfer Data Start Job called")
        // TODO Assure that the runs are processed only once. Verify via Process?
        List<Run> runs = Run.list()
        //runs.each { Run run ->
            createProcess(new ProcessParameter(value: runs[N].id.toString(), className: runs[N].class.name))
            println runs[N].toString()
        //}
        N++
        //performed = true
    }
}