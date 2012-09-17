package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CheckViewByPidJob extends AbstractEndStateAwareJobImpl {

    /**
     * dependency injection of meta data service
     */
    @Autowired
    FilesCompletenessService filesCompletenessService

    @Autowired
    SeqTrackService seqTrackService

    /**
     * Check if all files are linked in the view by pid structure
     *
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        if (filesCompletenessService.checkViewByPid(run)) {
            seqTrackService.setRunReadyForFastqc(run)
            succeed()
        } else {
            fail()
        }
    }
}
