package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class MergingCreateOutputDirectoryJob extends AbstractEndStateAwareJobImpl {

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

        assert mergingPass.mergingSet.status == MergingSet.State.INPROGRESS

        String dir = processedMergedBamFileService.directory(mergingPass)
        Realm realm = mergingPassService.realmForDataProcessing(mergingPass)
        executeOnRealm(dir, realm)

        File file = new File(dir)
        if (!file.canRead()) {
            log.debug "directory not readable ${file}".toString()
            throw new DirectoryNotReadableException("${file}")
        } else {
            succeed()
        }
    }

    private void executeOnRealm(String directory, Realm realm) {
        String cmd = "umask 027; mkdir -p -m 2750 " + directory
        String exitCode = executionService.executeCommand(realm, cmd)
        log.debug "creating directory finished with the output " + exitCode
    }
}
