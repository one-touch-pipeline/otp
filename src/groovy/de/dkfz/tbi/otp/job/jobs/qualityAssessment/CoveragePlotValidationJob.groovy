package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CoveragePlotValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long processedBamFileId = Long.parseLong(getProcessParameterValue())
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(processedBamFileId)
        boolean coveragePlotCreated = validateCoveragePlotAndUpdateProcessedBamFileStatus(processedBamFile)
        coveragePlotCreated ? succeed() : fail()
    }

    private boolean validateCoveragePlotAndUpdateProcessedBamFileStatus(ProcessedBamFile processedBamFile) {
        String coveragePlotFilePath = processedBamFileQaFileService.coveragePlotFilePath(processedBamFile)
        File file = new File(coveragePlotFilePath)
        processedBamFile.hasCoveragePlot = file.canRead() && file.size() != 0
        processedBamFile.save(flush: true)
        return processedBamFile.hasCoveragePlot
    }
}
