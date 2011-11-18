package de.dkfz.tbi.otp.job.jobs

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl

@Component("loadMetaDataJob")
@Scope("prototype")
class LoadMetaDataJob extends AbstractJobImpl {

   /**
    * dependency injection of meta data service
    */
   def metaDataService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getParameterValueOrClass("Run"))
        metaDataService.loadMetaData(runId)
    }
}
