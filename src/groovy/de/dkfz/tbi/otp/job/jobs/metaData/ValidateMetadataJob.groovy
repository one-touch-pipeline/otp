package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.MetaDataService

class ValidateMetadataJob extends AbstractEndStateAwareJobImpl {

   /**
    * dependency injection of meta data service
    */
    @Autowired
    MetaDataService metaDataService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        if (metaDataService.validateMetadata(runId)) {
            succeed()
        } else {
            fail()
        }
    }
}
