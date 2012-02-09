package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.MetaDataValidationService

class ValidateMetadataJob extends AbstractEndStateAwareJobImpl {

   /**
    * dependency injection of meta data service
    */
    @Autowired
    MetaDataValidationService metaDataValidationService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        if (metaDataValidationService.validateMetadata(runId)) {
            succeed()
        } else {
            fail()
        }
    }
}
