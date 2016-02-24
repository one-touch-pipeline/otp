package de.dkfz.tbi.otp.example

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.EndStateAwareJob
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.ResumableJob

@ResumableJob
class CompareJob extends AbstractJobImpl implements EndStateAwareJob {
    private ExecutionState endState = null

    @Override
    public void execute() throws Exception {
        String value1 = getParameterValueOrClass("value1")
        String value2 = getParameterValueOrClass("value2")
        if (value1 == value2) {
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
