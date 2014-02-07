package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.ngsdata.*

class SendIndexingBamJob extends AbstractJobImpl {

    @Autowired
    ExecutionHelperService executionHelperService

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
        String jobId = executionHelperService.sendScript(realm, text)
        return jobId
    }

    private String buildScriptText(DataFile file) {
        log.debug file.fileName
        String path = lsdfFilesService.getFileViewByPidDirectory(file)
        if (!path) {
            throw new Exception("View-by-Pid location not defined for file id = ${file.id}")
        }
        log.debug path
        String inFile = file.fileName
        String outFile = "${inFile}.bai"
        String cmd = "cd ${path}; samtools index ${inFile} ${outFile}.bai;chmod 440 ${outFile}"
        return cmd
    }
}
