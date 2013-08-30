package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateQaOutputDirectoryJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Autowired
    QualityAssessmentPassService qualityAssessmentPassService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)

        // TODO remove this hack when multiple start is solved
        qualityAssessmentPassService.passStarted(pass)

        String dir = processedBamFileQaFileService.directoryPath(pass)
        Realm realm = qualityAssessmentPassService.realmForDataProcessing(pass)
        execute(dir, realm)
    }

    private void execute(String directory, Realm realm) {
        String cmd = "mkdir -p " + directory
        log.debug cmd
        String exitCode = executionService.executeCommand(realm, cmd)
        boolean dirCreated = validate(directory)
        dirCreated ? succeed() : fail()
    }

    private boolean validate(String dirPath) {
        File folder = new File(dirPath)
        return folder.exists()
    }
}
