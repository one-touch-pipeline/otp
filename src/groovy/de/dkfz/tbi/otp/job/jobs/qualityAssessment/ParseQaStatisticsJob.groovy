package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class ParseQaStatisticsJob extends AbstractJobImpl {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)
        QualityAssessmentPass.withTransaction {
            abstractQualityAssessmentService.parseQaStatistics(pass)
            abstractQualityAssessmentService.saveCoverageToProcessedBamFile(pass)
        }
    }
}
