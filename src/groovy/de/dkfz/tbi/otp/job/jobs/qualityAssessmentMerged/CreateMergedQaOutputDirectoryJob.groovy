package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateMergedQaOutputDirectoryJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)

        // TODO remove this hack when multiple start is solved
        qualityAssessmentMergedPassService.passStarted(pass)

        String directory = processedMergedBamFileQaFileService.directoryPath(pass)
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
        String cmd = "mkdir -p -m 2750 " + directory
        String exitCode = executionService.executeCommand(realm, cmd)
        boolean dirCreated = validate(directory)
        dirCreated ? succeed() : fail()
    }

    private boolean validate(String dirPath) {
        File folder = new File(dirPath)
        return folder.exists()
    }
}

