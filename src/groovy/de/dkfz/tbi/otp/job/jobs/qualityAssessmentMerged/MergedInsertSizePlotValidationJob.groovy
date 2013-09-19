package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class MergedInsertSizePlotValidationJob  extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        boolean plotCreated = processedMergedBamFileQaFileService.validateInsertSizePlotAndUpdateProcessedMergedBamFileStatus(pass)
        plotCreated ? succeed() : fail()
    }
}
