package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.*

class CheckIndexFileJob  extends AbstractEndStateAwareJobImpl {

    SeqScan scan

    @Autowired
    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        scan = SeqScan.get(scanId)
        List<DataFile> alignFiles = mergedAlignmentDataFileService.alignmentSequenceFiles(scan)
        for (DataFile file in alignFiles) {
            String path = lsdfFilesService.getFileViewByPidPath(file)
            path = "${path}.bai"
            log.debug path
            if (!lsdfFilesService.fileExists(path)) {
                fail()
                return
            }
        }
        succeed()
    }
}
