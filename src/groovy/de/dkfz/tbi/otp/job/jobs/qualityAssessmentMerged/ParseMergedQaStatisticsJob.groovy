package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class ParseMergedQaStatisticsJob extends AbstractJobImpl {

    @Autowired
    AbstractQualityAssessmentService abstractQualityAssessmentService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        QualityAssessmentMergedPass.withTransaction {
            abstractQualityAssessmentService.parseQaStatistics(pass)
            abstractQualityAssessmentService.saveCoverageToProcessedMergedBamFile(pass)
        }
    }
}
