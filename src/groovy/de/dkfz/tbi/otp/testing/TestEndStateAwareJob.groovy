package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.StartJob
import de.dkfz.tbi.otp.job.processing.EndStateAwareJob

import org.apache.tools.ant.types.selectors.ExtendSelector
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple Test implementation of the EndStateAware interface.
 * Does nothing useful.
 *
 */
@Component("testEndStateAwareJob")
@Scope("prototype")
class TestEndStateAwareJob extends AbstractJobImpl implements EndStateAwareJob {

    @Override
    public ExecutionState getEndState() {
        println("Getter for end state of TestEndStateAwareJob called")
        ExecutionState.SUCCESS
    }

    @Override
    public void execute() throws Exception {
        println("Execute method of TestEndStateAwareJob called")
        addOutputParameter("test", "1234")
        addOutputParameter("test2", "1234")
        addOutputParameter("test2", "4321")
    }

    @Override
    public String getVersion() {
        return ""
    }
}
