package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class ParseQaStatisticsJob extends AbstractJobImpl {

    @Autowired
    QualityAssessmentProcessingService qualityAssessmentProcessingService

    @Override
    public void execute() throws Exception {
        long processedBamFileId = Long.parseLong(getProcessParameterValue())
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(processedBamFileId)
        qualityAssessmentProcessingService.parseQaStatistics(processedBamFile)
    }
}
