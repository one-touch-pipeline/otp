package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.*

class SendIndexingBamJob extends AbstractJobImpl {

    @Autowired
    ExecutionService executionService

    @Autowired
    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        SeqScan scan = SeqScan.get(scanId)
        String jobIds = processScan(scan)
        addOutputParameter("pbsIds", jobIds)
    }

    private String processScan(SeqScan scan) {
        String jobIds = ""
        List<DataFile> alignFiles = mergedAlignmentDataFileService.alignmentSequenceFiles(scan)
        for(DataFile file in alignFiles) {
            jobIds += processFile(file)
            jobIds += ","
        }
        return jobIds
    }

    private String processFile(DataFile file) {
        Realm realm = file.project.realm
        String text = buildScriptText(file)
        String jobId = sendScript(realm, text)
        return jobId
    }

    private String buildScriptText(DataFile file) {
        println file.fileName
        String path = lsdfFilesService.getFileViewByPidDirectory(file)
        if (!path) {
            throw new Exception("View-by-Pid location not defined for file id = ${file.id}")
        }
        println path
        String inFile = file.fileName
        String outFile = "${inFile}.bai"
        String cmd = "cd ${path}; samtools index ${inFile} ${outFile}.bai;chmod 440 ${outFile}"
        println cmd
        return cmd
    }

    private String sendScript(Realm realm, String text) {
        return "1"
        String pbsResponse = executionService.executeJob(realm, text)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            println "Number of PBS is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }
}
