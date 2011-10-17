package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Very simple TestJob, just printing something to sysout.
 *
 */
@Component("testJob")
@Scope("prototype")
class TestJob extends AbstractJobImpl {
    @Override
    public void execute() throws Exception {
        println("Execute method of TestJob called")
        addOutputParameter(new Parameter(key: "test", value: "1234", referencesDomainClass: false))
        addOutputParameter(new Parameter(key: "test2", value: "1234", referencesDomainClass: false))
        addOutputParameter(new Parameter(key: "test2", value: "4321", referencesDomainClass: true))
    }
}
