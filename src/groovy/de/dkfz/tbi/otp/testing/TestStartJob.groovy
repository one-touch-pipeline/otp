package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple Test implementation of the StartJob interface.
 * Does nothing useful.
 *
 */
@Component("testStartJob")
@Scope("prototype")
class TestStartJob extends AbstractStartJobImpl {

    @SuppressWarnings("EmptyMethod")
    void execute() {
    }

    @Override
    String getJobExecutionPlanName() {
        return null
    }
}
