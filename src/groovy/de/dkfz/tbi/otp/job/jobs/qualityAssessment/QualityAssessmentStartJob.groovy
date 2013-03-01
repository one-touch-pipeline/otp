package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("qualityAssessmentStartJob")
@Scope("singleton")
class QualityAssessmentStartJob extends AbstractStartJobImpl {

    @Autowired
    QualityAssessmentProcessingService qualityAssessmentProcessingService

    @Autowired
    ProcessingOptionService optionService

    final int MAX_RUNNING = 4

    @Scheduled(fixedRate=10000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        ProcessedBamFile processedBamFile = qualityAssessmentProcessingService.bamFileReadyForQa()
        if (processedBamFile) {
            log.debug "Creating alignment quality assessment process for ${processedBamFile}"
            qualityAssessmentProcessingService.setQaInProcessing(processedBamFile)
            createProcess(new ProcessParameter(value: processedBamFile.id.toString(), className: processedBamFile.class.name))
        }
    }

    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "QualityAssessmentWorkflow", null, MAX_RUNNING)
        return (n < maxRunning)
    }
}
