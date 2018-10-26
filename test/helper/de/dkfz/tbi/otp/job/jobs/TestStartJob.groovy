package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple Test implementation of the StartJob interface, does nothing useful.
 */
@Component("testStartJob")
@Scope("prototype")
class TestStartJob extends AbstractStartJobImpl {

    @SuppressWarnings("EmptyMethod")
    @Override
    void execute() {
    }
}
