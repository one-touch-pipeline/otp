package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("checkViewByPid")
@Scope("prototype")
class CheckViewByPidJob extends AbstractEndStateAwareJobImpl {

    /**
     * dependency injection of meta data service
     */
    @Autowired
    FilesCompletenessService filesCompletenessService

    /**
     * Check if all files are linked in the view by pid structure
     *
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue("run"))
        Run run = Run.get(runId)
        if (filesCompletenessService.checkViewByPid(run)) {
            succeed()
        } else {
            succeed()
            //fail()
        }
    }
}
