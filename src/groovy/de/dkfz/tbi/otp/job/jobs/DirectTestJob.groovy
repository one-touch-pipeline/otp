package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ProcessingStep

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Simple test implementing the Job interface without inheriting
 * AbstractJob.
 *
 */
@Component("directTestJob")
@Scope("prototype")
class DirectTestJob implements Job {
    private ProcessingStep step

    DirectTestJob() {}
    DirectTestJob(ProcessingStep step, Set unused) {
        this.step = step
    }

    @Override
    public void execute() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<Parameter> getOutputParameters() throws InvalidStateException {
        return [new Parameter(value: "abcd", type: ParameterType.findByJobDefinition(step.jobDefinition))]
    }

    @Override
    public ProcessingStep getProcessingStep() {
        return step
    }

    @Override
    public void start() throws InvalidStateException {
        // TODO Auto-generated method stub
    }

    @Override
    public void end() throws InvalidStateException {
        // TODO Auto-generated method stub
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return ""
    }

}
