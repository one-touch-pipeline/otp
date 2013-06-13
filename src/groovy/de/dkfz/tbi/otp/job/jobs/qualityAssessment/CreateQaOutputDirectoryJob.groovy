package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateQaOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    @Override
    public void execute() throws Exception {
//        long processedBamFileId = Long.parseLong(getProcessParameterValue())
        long processedBamFileId = getProcessParameterValue() as long
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(processedBamFileId)
        String dir = processedBamFileQaFileService.directoryPath(processedBamFile)
//        Realm realm = configService.getRealmDataProcessing(processedBamFile.alignmentPass.seqTrack.sample.individual.project)
        Realm realm = ProcessedBamFileService.realm(processedBamFile)
        execute(dir, realm)
    }

    private void execute(String directory, Realm realm) {
        String cmd = "mkdir -p " + directory
        String exitCode = executionService.executeCommand(realm, cmd)
        log.debug "creating directory finished with exit code " + exitCode
    }
}
