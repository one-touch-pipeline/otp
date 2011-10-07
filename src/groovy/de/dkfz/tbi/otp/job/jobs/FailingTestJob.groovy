package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.scheduler.JobExecution
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Scope

/**
 * Very simple test job which just throws an Exception during Execution.
 *
 */
@Scope("prototype")
@Component("failingTestJob")
class FailingTestJob implements Job {
    ProcessingStep processingStep
    
    @JobExecution
    @Override
    public void execute() throws Exception {
        throw new Exception("Testing")
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
