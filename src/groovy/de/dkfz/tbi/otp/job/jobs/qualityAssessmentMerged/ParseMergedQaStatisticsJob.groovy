package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class ParseMergedQaStatisticsJob extends AbstractJobImpl {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        QualityAssessmentMergedPass.withTransaction {
            abstractQualityAssessmentService.parseQaStatistics(pass)
            abstractQualityAssessmentService.saveCoverageToAbstractMergedBamFile(pass)
        }
    }
}
