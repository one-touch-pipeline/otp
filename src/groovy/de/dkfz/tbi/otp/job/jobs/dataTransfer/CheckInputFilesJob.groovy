package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("checkInputFiles")
@Scope("prototype")
class CheckInputFilesJob extends AbstractEndStateAwareJobImpl {

    /**
     * dependency injection of meta data service
     */
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
        long runId = Long.parseLong(getProcessParameterValue("run"))
        Run run = Run.get(runId)
        if (filesCompletenessService.checkInitialSequenceFiles(run)) {
            println "OK"
            succeed()
        } else {
            println "Fails"
            fail()
        }
    }
}
