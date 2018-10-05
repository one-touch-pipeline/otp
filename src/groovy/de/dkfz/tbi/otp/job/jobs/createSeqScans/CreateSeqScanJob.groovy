package de.dkfz.tbi.otp.job.jobs.createSeqScans

import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
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
    void execute() throws Exception {
        long seqTrackId = Long.parseLong(getProcessParameterValue())
        SeqTrack seqTrack = SeqTrack.get(seqTrackId)
        seqScanService.buildSeqScan(seqTrack)
        succeed()
    }
}
