package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

/**
 * Simple test implementing the Job interface without inheriting
 * AbstractJob.
 */
@Component
@Scope("prototype")
@UseJobLog
class DirectTestJob implements Job {
    private ProcessingStep step

    @Override
    public void execute() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public Set<Parameter> getOutputParameters() throws InvalidStateException {
        return [new Parameter(value: "abcd", type: ParameterType.findByJobDefinition(step.jobDefinition))]
    }

    @Override
    void setProcessingStep(ProcessingStep step) {
        this.step = step
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
}
