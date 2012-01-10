package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.SeqTrackService

class CheckSequenceTracksJob extends AbstractEndStateAwareJobImpl {

    /**
     * dependency injection of seqTrack service
     */
    @Autowired
    SeqTrackService seqTrackService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        seqTrackService.checkSequenceTracks(runId)
        succeed()
    }
}
