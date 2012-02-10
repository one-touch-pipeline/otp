package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.ngsdata.RunDateParserService

class BuildExecutionDateJob extends AbstractJobImpl {

   /**
    * dependency injection of meta data service
    */
   @Autowired
   RunDateParserService runDateParserService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        runDateParserService.buildExecutionDate(runId)
    }
}
