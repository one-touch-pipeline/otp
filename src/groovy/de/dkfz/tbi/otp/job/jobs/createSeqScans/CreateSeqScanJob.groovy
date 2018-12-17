package de.dkfz.tbi.otp.job.jobs.createSeqScans

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.job.ast.UseJobLog
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.SeqScanService
import de.dkfz.tbi.otp.ngsdata.SeqTrack

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
