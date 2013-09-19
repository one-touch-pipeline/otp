package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class MergedCoveragePlotValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        boolean coveragePlotCreated = processedMergedBamFileQaFileService.validateCoveragePlotAndUpdateProcessedMergedBamFileStatus(pass)
        coveragePlotCreated ? succeed() : fail()
    }
}
