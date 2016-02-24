package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.*

@Component("dataInstallationStartJob")
@Scope("singleton")
class DataInstallationStartJob extends AbstractStartJobImpl {

    @Autowired
    RunProcessingService runProcessingService

    @Autowired
    ProcessingOptionService optionService

    @Scheduled(fixedDelay=5000l)
    void execute() {
        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }
        Run.withTransaction {
            Run run = runProcessingService.runReadyToInstall(minPriority)
            if (run) {
                runProcessingService.blockInstallation(run)
                createProcess(run)
                log.debug "Installing Run: ${run.name}"
            }
        }
    }

    @Override
    protected String getJobExecutionPlanName() {
        return "DataInstallationWorkflow"
    }
}
