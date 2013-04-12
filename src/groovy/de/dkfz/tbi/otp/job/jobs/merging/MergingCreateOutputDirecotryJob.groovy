package de.dkfz.tbi.otp.job.jobs.merging

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class MergingCreateOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    MergingPassService mergingPassService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        String dir = processedMergedBamFileService.getDirectory(mergingPass)
        Realm realm = mergingPassService.realmForDataProcessing(mergingPass)
        executeOnRealm(dir, realm)
    }

    private void executeOnRealm(String directory, Realm realm) {
        String cmd = "mkdir -p " + directory
        String exitCode = executionService.executeCommand(realm, cmd)
        log.debug "creating directory finished with exit code " + exitCode
    }
}



