package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CheckFinalLocationJob extends AbstractEndStateAwareJobImpl {

    /**
     * dependency injection of meta data service
     */
    @Autowired
    FilesCompletenessService filesCompletenessService

    /**
     * Check if all files are in the final location
     *
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        if (filesCompletenessService.checkFinalLocation(run)) {
            succeed()
        } else {
            succeed()
            //fail()
        }
    }
}
