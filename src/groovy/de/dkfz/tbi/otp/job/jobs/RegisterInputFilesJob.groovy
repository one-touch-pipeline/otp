package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

@Component("registerInputFilesJob")
@Scope("prototype")
class RegisterInputFilesJob extends AbstractJobImpl {

    /**
     * dependency injection of meta data service
     */
    def metaDataService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getParameterValueOrClass("Run"))
        metaDataService.registerInputFiles(runId)
    }
}
