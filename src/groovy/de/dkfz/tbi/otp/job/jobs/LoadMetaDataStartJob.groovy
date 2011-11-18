package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter;
import de.dkfz.tbi.otp.job.processing.ParameterType;
import de.dkfz.tbi.otp.ngsdata.Run
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("exampleStartJob")
@Scope("singleton")
class LoadMetaDataStartJob extends AbstractStartJobImpl {
    boolean performed = false

    @Scheduled(fixedRate=10000l)
    void execute() {
        if (performed) {
            return
        }
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            println("Execution plan not set or not active")
            return
        }
        println("Load Meta Data Start Job called")
        List<Run> runs = Run.findAll()
        runs.each { Run run ->
            schedulerService.createProcess(
                this, [
                new Parameter(
                    type: ParameterType.findByNameAndJobDefinition("Run", getExecutionPlan().startJob), 
                    value: run.id.toString()
                )
            ])
            println run.name
        }



        /*        // processing and poor's man time measurement
         def times = []
         println run.name
         times << new Date().getTime()
         metaDataService.registerInputFiles(id)
         times << new Date().getTime()
         metaDataService.loadMetaData(id)
         times << new Date().getTime()
         metaDataService.validateMetadata(id)
         times << new Date().getTime()
         metaDataService.buildExecutionDate(id)
         times << new Date().getTime()
         metaDataService.buildSequenceTracks(id)
         times << new Date().getTime()
         metaDataService.checkSequenceTracks(id)
         times << new Date().getTime()
         println "total ${times[6]-times[0]}"*/
        performed = true
    }
}
