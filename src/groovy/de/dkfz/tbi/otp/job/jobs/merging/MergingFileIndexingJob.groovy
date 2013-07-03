package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class MergingFileIndexingJob extends AbstractJobImpl {

    @Autowired
    MergingPassService mergingPassService

    @Autowired
    ProcessedMergingFileService processedMergingFileService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ExecutionHelperService executionHelperService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)
        Realm realm = mergingPassService.realmForDataProcessing(mergingPass)
        String cmd = createIndexingCommand(mergedBamFile)
        String pbsId = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsId)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String createIndexingCommand(ProcessedMergedBamFile bamFile) {
        String path = processedMergingFileService.directory(bamFile)
        String fileName = processedMergedBamFileService.fileName(bamFile)
        String baiFileName = processedMergedBamFileService.fileNameForBai(bamFile)
        return "cd ${path}; samtools index ${fileName}; chmod 440 ${baiFileName}"
    }
}
