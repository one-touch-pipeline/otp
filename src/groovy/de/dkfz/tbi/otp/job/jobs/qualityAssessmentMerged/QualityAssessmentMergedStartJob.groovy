package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("qualityAssessmentMergedStartJob")
@Scope("singleton")
class QualityAssessmentMergedStartJob extends AbstractStartJobImpl {

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    ProcessingOptionService optionService

    final int MAX_RUNNING = 4

    @Scheduled(fixedDelay = 10000l)
    void execute() {
        short minPriority = minimumProcessingPriorityForOccupyingASlot

        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }

        QualityAssessmentMergedPass.withTransaction {
            QualityAssessmentMergedPass qualityAssessmentMergedPass = qualityAssessmentMergedPassService.createPass(minPriority)
            if (qualityAssessmentMergedPass) {
                log.debug "Creating merged quality assessment process for ${qualityAssessmentMergedPass}"
                qualityAssessmentMergedPassService.passStarted(qualityAssessmentMergedPass)
                createProcess(qualityAssessmentMergedPass)
            }
        }
    }

    @Override
    protected String getJobExecutionPlanName() {
        return "QualityAssessmentMergedWorkflow"
    }
}
