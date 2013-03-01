package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class InsertSizePlotValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long processedBamFileId = Long.parseLong(getProcessParameterValue())
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(processedBamFileId)
        boolean plotCreated = validateAndUpdateProcessedBamFileStatus(processedBamFile)
        plotCreated ? succeed() : fail()
    }

    private boolean validateAndUpdateProcessedBamFileStatus(ProcessedBamFile processedBamFile) {
        String plotFilePath = processedBamFileQaFileService.insertSizePlotFilePath(processedBamFile)
        File file = new File(plotFilePath)
        processedBamFile.hasInsertSizePlot = file.canRead() && file.size() != 0
        processedBamFile.save(flush: true)
        return processedBamFile.hasInsertSizePlot
    }
}
