package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.scheduler.SchedulerService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("exampleStartJob")
@Scope("singleton")
class ExampleStartJob extends AbstractStartJobImpl {
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
        println("Example Start Job called")
        Parameter directoryParameter = getStartJobDefinition().constantParameters.find { it.type.name == "directory" }
        if (!directoryParameter) {
            println("Required parameter not found")
            return
        }
        File directory = new File(directoryParameter.value)
        if (!directory.exists() || !directory.isDirectory()) {
            println("Directory parameter does not point to directory")
            return
        }
        directory.listFiles(new FileFilter() {
            boolean accept(File file) {
                return file.exists() && file.isFile() && file.name.endsWith("tar.gz")
            }
        }).each {
            schedulerService.createProcess(this, [
                new Parameter(type: ParameterType.findByNameAndJobDefinition("file", getExecutionPlan().startJob), value: it.absolutePath)
                ])
            println it.name
        }
        performed = true
    }
}
