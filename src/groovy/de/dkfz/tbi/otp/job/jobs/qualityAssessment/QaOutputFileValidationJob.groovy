package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class QaOutputFileValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(Long.parseLong(getProcessParameterValue()))
        String coverateDataFilePath = processedBamFileQaFileService.coverageDataFilePath(processedBamFile)
        String qualityAssessmentFilePath = processedBamFileQaFileService.qualityAssessmentDataFilePath(processedBamFile)
        String insertSizeDataFilePath = processedBamFileQaFileService.insertSizeDataFilePath(processedBamFile)
        boolean coverageDataFileExists = validateFile(coverateDataFilePath)
        boolean qualityAssessmentFileExists = validateFile(qualityAssessmentFilePath)
        boolean insertSizeDataFileExists = validateFile(insertSizeDataFilePath)
        boolean state = coverageDataFileExists && qualityAssessmentFileExists && insertSizeDataFileExists
        state ? succeed() : fail()
    }

    private boolean validateFile(String path) {
        File file = new File(path)
        return file.canRead() && file.size() != 0
    }
}
