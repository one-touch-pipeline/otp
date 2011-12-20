package de.dkfz.tbi.otp.job.jobs.metaData

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.Run
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("metaDataStartJob")
@Scope("singleton")
class MetaDataStartJob extends AbstractStartJobImpl {
    boolean performed = false

    @Scheduled(fixedRate=10000l)
    void execute() {
        if (performed) {
            println "Jobs already started ..."
            return
        }
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            println("Execution plan not set or not active")
            return
        }
        //return
        println("Load Meta Data Start Job called")
        // TODO Assure that the runs are processed only once. Verify via Process?
        List<Run> runs = Run.list()
        runs.each { Run run ->
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
            println run.toString()
        }
        performed = true
    }
}
