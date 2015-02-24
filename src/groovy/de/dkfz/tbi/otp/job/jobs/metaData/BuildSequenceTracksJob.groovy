package de.dkfz.tbi.otp.job.jobs.metaData

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.ngsdata.*

class BuildSequenceTracksJob extends AbstractJobImpl {

    /**
     * dependency injection of seqTrack service
     */
    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    MultiplexingService multiplexingService

    @Autowired
    MetaDataService metaDataService

    @Override
    public void execute() throws Exception {
        SeqTrack.withTransaction {
            long runId = Long.parseLong(getProcessParameterValue())
            Run run = Run.get(runId)
            if (multiplexingService.needsMultiplexingHandling(run)) {
                multiplexingService.executeMultiplexing(run)
                log.debug "Multiplexing service started for run ${run.name}"
            }
            seqTrackService.buildSequenceTracks(runId)
            metaDataService.enrichOldDataWithNewInformationFrom(run)
        }
    }
}
