package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CheckInputFilesJob extends AbstractJobImpl {

    @Autowired
    FilesCompletenessService filesCompletenessService

    /**
     * Check if all sequence files belonging to this run are
     * present in the initial location. The job is delegated to service
     *
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        filesCompletenessService.checkInitialSequenceFiles(run)
    }
}
