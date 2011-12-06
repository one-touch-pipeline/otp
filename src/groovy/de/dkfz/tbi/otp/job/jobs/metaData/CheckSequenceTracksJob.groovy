package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl;
import de.dkfz.tbi.otp.ngsdata.SeqTrackService

@Component("checkSequenceTracksJob")
@Scope("prototype")
class CheckSequenceTracksJob extends AbstractJobImpl {

   /**
    * dependency injection of seqTrack service
    */
    @Autowired
    SeqTrackService seqTrackService

    @Override
    public void execute() throws Exception {
        long runId = getProcessParameterValue("run")
        seqTrackService.checkSequenceTracks(runId)
    }
}
