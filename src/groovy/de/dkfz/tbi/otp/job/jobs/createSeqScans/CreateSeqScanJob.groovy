package de.dkfz.tbi.otp.job.jobs.createSeqScans

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateSeqScanJob extends AbstractEndStateAwareJobImpl {

    /**
     * dependency injection of meta data service
     */
    @Autowired
    SeqScanService seqScanService

    /**
     * Check if all files are in the final location
     *
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        long seqTrackId = Long.parseLong(getProcessParameterValue())
        SeqTrack seqTrack = SeqTrack.get(seqTrackId)
        seqScanService.buildSeqScan(seqTrack)
        succeed()
    }
}
