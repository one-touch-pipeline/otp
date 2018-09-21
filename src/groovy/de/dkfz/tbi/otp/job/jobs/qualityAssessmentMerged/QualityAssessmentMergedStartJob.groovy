package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*

@Component("qualityAssessmentMergedStartJob")
@Scope("singleton")
class QualityAssessmentMergedStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    ProcessingOptionService optionService

    @Scheduled(fixedDelay = 10000l)
    @Override
    void execute() {
        doWithPersistenceInterceptor {
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
    }

    @Override
    Process restart(Process process) {
        assert process

        QualityAssessmentMergedPass failedInstance = (QualityAssessmentMergedPass)process.getProcessParameterObject()

        QualityAssessmentMergedPass.withTransaction {
            QualityAssessmentMergedPass newQualityAssessmentMergedPass = new QualityAssessmentMergedPass(
                    abstractMergedBamFile: failedInstance.abstractMergedBamFile,
                    identifier: QualityAssessmentMergedPass.nextIdentifier(failedInstance.abstractMergedBamFile),
            ).save(flush: true)
            qualityAssessmentMergedPassService.passStarted(newQualityAssessmentMergedPass)

            return createProcess(newQualityAssessmentMergedPass)
        }
    }
}
