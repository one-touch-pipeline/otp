package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl;

@Component("checkSequenceTracksJob")
@Scope("prototype")
class CheckSequenceTracksJob extends AbstractJobImpl {

   /**
    * dependency injection of meta data service
    */
   def metaDataService

    @Override
    public void execute() throws Exception {
        long runId = getParameterValueOrClass("run")
        metaDataService.checkSequenceTracks(runId)
    }
}
