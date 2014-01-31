package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.*

//This Job is not used anymore in the workflow "IndexSingleBamWorkflow".
//It will be kept because of historical reasons and perhaps can be reused later.
@Deprecated
class CreateSingleBamDirectoryJob extends AbstractJobImpl {

    SeqScan scan

    @Autowired
    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        scan = SeqScan.get(scanId)
        Realm realm = scan.sample.individual.project.realm
        String text = buildScriptText()
        log.debug text
        executionService.executeCommand(realm, text)
    }

    private String buildScriptText() {
        MergedAlignmentDataFile dataFile = getDataFile()
        String basePath = mergedAlignmentDataFileService.pathToHost(scan)
        String filePath = dataFile.filePath
        String path = "${basePath}/${filePath}"
        String text = "mkdir -p -m 0750 ${path}"
    }

    private MergedAlignmentDataFile getDataFile() {
        MergingLog mergingLog = MergingLog.findBySeqScan(scan)
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(mergingLog)
        return dataFile
    }
}
