package de.dkfz.tbi.otp.job.jobs.dataTransfer

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.jobs.WatchdogJob
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.ngsdata.*

public class CalculateChecksumJob extends AbstractJobImpl {

    final String paramName = "__pbsIds"

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ConfigService configService

    @Autowired
    RunProcessingService runProcessingService

    @Override
    public void execute() throws Exception {
        Run run = Run.get(Long.parseLong(getProcessParameterValue()))
        List<DataFile> files = runProcessingService.dataFilesForProcessing(run)

        List<String> pbsIds = []
        for (DataFile file in files) {
            if (checksumFileService.md5sumFileExists(file)) {
                log.debug "checksum file already exists for file ${file}".toString()
                continue
            }
            String cmd = scriptText(file)
            Realm realm = configService.getRealmDataManagement(file.project)
            String jobId = executionHelperService.sendScript(realm, cmd, this.getClass().simpleName)
            pbsIds << jobId
        }
        if (pbsIds.empty) {
            pbsIds << WatchdogJob.SKIP_WATCHDOG
        }
        addOutputParameter(paramName, pbsIds.join(","))
    }

    private String scriptText(DataFile file) {
        String directory = checksumFileService.dirToMd5File(file)
        String fileName = file.fileName
        String md5FileName = checksumFileService.md5FileName(file)
        String text = "cd ${directory};md5sum ${fileName} > ${md5FileName};chmod 440 ${md5FileName}"
        return text
    }
}
