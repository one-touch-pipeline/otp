package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.ngsdata.*

public class CompareChecksumJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    RunProcessingService runProcessingService

    @Override
    public void execute() throws Exception {
        Run run = Run.get(Long.parseLong(getProcessParameterValue()))
        List<DataFile> files = runProcessingService.dataFilesForProcessing(run)

        boolean allOk = true
        for (DataFile file in files) {
            log.debug "Checking ${file.fileName} ${file.md5sum}"
            if (!checksumFileService.compareMd5(file)) {
                allOk = false
                log.error "md5sum is inconsistent for file: ${file.fileName}"
            }
        }
        if (allOk) {
            succeed()
        } else {
            throw new RuntimeException('At least one md5sum is incorrect. See the job log for details.')
        }
    }
}
