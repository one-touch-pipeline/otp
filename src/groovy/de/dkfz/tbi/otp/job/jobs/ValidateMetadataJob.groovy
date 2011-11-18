package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

@Component("validateMetadataJob")
@Scope("prototype")
class ValidateMetadataJob extends AbstractJobImpl {

   /**
    * dependency injection of meta data service
    */
   def metaDataService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getParameterValueOrClass("run"))
        metaDataService.validateMetadata(runId)
    }
}
