package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple TestJob, just printing something to sysout.
 *
 */
@Component("testJob")
@Scope("prototype")
class TestJob extends AbstractJobImpl {
    @Override
    public void execute() throws Exception {
        println("Execute method of TestJob called")
        // TODO: ParameterType should not be saved in the test
        ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: getProcessingStep().jobDefinition)
        ParameterType test2 = new ParameterType(name: "test2", description: "Test description", jobDefinition: getProcessingStep().jobDefinition)
        test.save()
        test2.save()
        addOutputParameter(new Parameter(type: test, value: "1234"))
        addOutputParameter(new Parameter(type: test2, value: "1234"))
        addOutputParameter(new Parameter(type: test2, value: "4321"))
    }
}
