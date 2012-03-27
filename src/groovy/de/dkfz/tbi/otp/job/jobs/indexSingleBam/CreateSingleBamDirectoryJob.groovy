package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.ngsdata.*

class CreateSingleBamDirectoryJob extends AbstractJobImpl {

    SeqScan scan

    @Autowired
    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        scan = SeqScan.get(scanId)
        String text = buildScriptText()
        println text
    }

    private String buildScriptText() {
        MergedAlignmentDataFile dataFile = getDataFile()
        String basePath = mergedAlignmentDataFileService.pathToHost(scan)
        String filePath = dataFile.filePath
        String path = "${basePath}/${filePath}"
        String text = "mkdir -p ${path}"
    }

    private MergedAlignmentDataFile getDataFile() {
        MergingLog mergingLog = MergingLog.findBySeqScan(scan)
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(mergingLog)
        return dataFile
    }
}
