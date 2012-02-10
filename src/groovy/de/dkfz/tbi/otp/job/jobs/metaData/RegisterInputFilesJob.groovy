package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.ngsdata.MetaDataRegistrationService

class RegisterInputFilesJob extends AbstractJobImpl {

    /**
     * dependency injection of meta data service
     */
    @Autowired
    MetaDataRegistrationService metaDataRegistrationService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        metaDataRegistrationService.registerInputFiles(runId)
    }
}
