package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.MetaDataRegistrationService

class RegisterInputFilesJob extends AbstractEndStateAwareJobImpl {

    /**
     * dependency injection of meta data service
     */
    @Autowired
    MetaDataRegistrationService metaDataRegistrationService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        boolean state = metaDataRegistrationService.registerInputFiles(runId)
        (state) ? succeed() : fail()
    }
}
