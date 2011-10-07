package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope

/**
 * Very simple TestJob, just printing something to sysout.
 *
 */
@Scope("prototype")
@Component("testJob")
class TestJob implements Job {
    ProcessingStep processingStep

    @Override
    public void execute() throws Exception {
        println("Execute method of TestJob called")
    }

    @Override
    public List<Parameter> getOutputParameters() throws InvalidStateException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProcessingStep getProcessingStep() {
        return processingStep
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

}
