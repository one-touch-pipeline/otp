package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.AbstractJobImpl;
import de.dkfz.tbi.otp.ngsdata.SeqTrackService

class BuildSequenceTracksJob extends AbstractJobImpl {

   /**
    * dependency injection of seqTrack service
    */
    @Autowired
    SeqTrackService seqTrackService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        seqTrackService.buildSequenceTracks(runId)
    }
}
