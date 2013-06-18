package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("exampleStartJob")
@Scope("singleton")
class ExampleStartJob extends AbstractStartJobImpl {
    boolean performed = false

    @Scheduled(fixedDelay=10000l)
    void execute() {
        if (performed) {
            return
        }
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
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
            if (isNewProcessAllowed()) {
                schedulerService.createProcess(this, [
                    new Parameter(type: ParameterType.findByNameAndJobDefinition("file", getExecutionPlan().startJob), value: it.absolutePath)
                    ])
                println it.name
            } else {
                println("No new processes are allowed by limit (${getExecutionPlan().numberOfAllowedProcesses}) set in JobExecutionPlan ${getExecutionPlan().name}")
            }
        }
        performed = true
    }
}
