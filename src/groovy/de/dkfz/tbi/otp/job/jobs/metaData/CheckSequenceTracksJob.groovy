package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl;

@Component("checkSequenceTracksJob")
@Scope("prototype")
class CheckSequenceTracksJob extends AbstractJobImpl {

   /**
    * dependency injection of seqTrack service
    */
   def seqTrackService

    @Override
    public void execute() throws Exception {
        long runId = getProcessParameterValue("run")
        seqTrackService.checkSequenceTracks(runId)
    }
}
