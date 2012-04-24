package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.*

class SendIndexingBamJob extends AbstractJobImpl {

    SeqScan scan

    @Autowired
    ExecutionService executionService

    @Autowired
    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        scan = SeqScan.get(scanId)
        Realm realm = scan.sample.individual.project.realm
        String text = buildScriptText()
        String jobId = sendScript(realm, text)
        println "Job ${jobId} submitted to PBS"
        addOutputParameter("pbsIds", jobId)
    }

    private String buildScriptText() {
        MergingLog mergingLog = MergingLog.findBySeqScan(scan)
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(mergingLog)
        String basePath = mergedAlignmentDataFileService.pathToHost(scan)
        String path = "${basePath}/${dataFile.filePath}"
        String cmd = "cd ${path}; samtools index ${dataFile.fileName} ${dataFile.fileName}.bai;chmod 440 ${dataFile.fileName}.bai"
        return cmd
    }

    private String sendScript(Realm realm, String text) {
        println text
        String pbsResponse = executionService.executeJob(realm, text)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            println "Number of PBS is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }
}
