package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.ngsdata.MetaDataService

@Component("metaDataJob")
@Scope("prototype")
class MetaDataJob extends AbstractJobImpl {

   /**
    * dependency injection of meta data service
    */
    @Autowired
    MetaDataService metaDataService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue("run"))
        metaDataService.loadMetaData(runId)
    }
}
