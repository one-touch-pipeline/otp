package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.EndStateAwareJob
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.Parameter
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("compareJob")
@Scope("prototype")
class CompareJob extends AbstractJobImpl implements EndStateAwareJob {
    private ExecutionState endState = null

    @Override
    public void execute() throws Exception {
        Parameter value1 = processingStep.input.find { it.type.name == "value1" }
        Parameter value2 = processingStep.input.find { it.type.name == "value2" }
        if (!value1 || !value2) {
            throw new RuntimeException("Required parameter not found")
        }
        if (value1.value == value2.value) {
            endState = ExecutionState.SUCCESS
        } else {
            endState = ExecutionState.FAILURE
        }
    }

    @Override
    public ExecutionState getEndState() throws InvalidStateException {
        if (!endState) {
            throw new InvalidStateException("End state not yet determined")
        }
        return endState
    }

}
