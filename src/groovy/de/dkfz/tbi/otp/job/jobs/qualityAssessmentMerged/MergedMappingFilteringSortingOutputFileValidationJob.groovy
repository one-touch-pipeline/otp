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
class MergedMappingFilteringSortingOutputFileValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        String filepath = processedMergedBamFileQaFileService.mappedFilteredSortedCoverageDataFilePath(pass)
        boolean fileCreated = validate(filepath)
        fileCreated ? succeed() : fail()
    }

    private boolean validate(String filepath) {
        File file = new File(filepath)
        return file.canRead() && file.size() != 0
    }
}
