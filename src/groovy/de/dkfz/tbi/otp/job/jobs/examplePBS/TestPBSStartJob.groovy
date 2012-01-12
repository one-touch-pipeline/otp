package de.dkfz.tbi.otp.job.jobs.examplePBS

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.Run
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("testPBSStartJob")
@Scope("singleton")
class TestPBSStatJob extends AbstractStartJobImpl  {


    @Scheduled(fixedRate=60000l)
    void execute() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            println("testPBS Execution plan not set or not active")
            return
        }
        createProcess(new ProcessParameter(value: "test"))
    }
}
