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
    void execute() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    Set<Parameter> getOutputParameters() throws InvalidStateException {
        return [new Parameter(value: "abcd", type: ParameterType.findByJobDefinition(step.jobDefinition))]
    }

    @Override
    void setProcessingStep(ProcessingStep step) {
        this.step = step
    }

    @Override
    ProcessingStep getProcessingStep() {
        return step
    }

    @Override
    void start() throws InvalidStateException {
        // TODO Auto-generated method stub
    }

    @Override
    void end() throws InvalidStateException {
        // TODO Auto-generated method stub
    }
}
