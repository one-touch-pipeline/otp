package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.ngsdata.*

class SendIndexingBamJob extends AbstractJobImpl {

    SeqScan scan

    //@Autowired
    //LsdfFilesService lsdfFilesService

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
        MergingLog mergingLog = MergingLog.findBySeqScan(scan)
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(mergingLog)
        String basePath = mergedAlignmentDataFileService.pathToHost(scan)
        String path = "${basePath}/${dataFile.filePath}"
        String cmd = "cd ${path}; samtools index ${dataFile.fileName} ${dataFile.fileName}.bai"
        return cmd
    }
}
