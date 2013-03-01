package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 */
class AssignQaFlagJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    QualityAssessmentProcessingService qualityAssessmentProcessingService

    @Override
    public void execute() throws Exception {
        long processedBamFileId = Long.parseLong(getProcessParameterValue())
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(processedBamFileId)
        qualityAssessmentProcessingService.setQaFinished(processedBamFile)
        succeed()
    }
}
