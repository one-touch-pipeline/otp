package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("checkFinalLocation")
@Scope("prototype")
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
        long runId = Long.parseLong(getProcessParameterValue("run"))
        Run run = Run.get(runId)
        if (filesCompletenessService.checkFinalLocation(run)) {
            succeed()
        } else {
            fail()
        }
    }
}
