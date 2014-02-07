package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class BamFileIndexingJob extends AbstractJobImpl {

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    AlignmentPassService alignmentPassService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ProcessingOptionService optionService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile bamFile = parseInput()
        Realm realm = alignmentPassService.realmForDataProcessing(bamFile.alignmentPass)
        String cmd = createIndexingCommand(bamFile)
        String pbsId = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsId)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private ProcessedBamFile parseInput() {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        String type = getParameterValueOrClass("BamType")
        return processedBamFileService.findBamFile(alignmentPassId, type)
    }

    private String createIndexingCommand(ProcessedBamFile bamFile) {
        String path = processedBamFileService.getDirectory(bamFile)
        String fileName = processedBamFileService.getFileName(bamFile)
        String baiFilePath = processedBamFileService.baiFilePath(bamFile)
        String samToolsBinary = optionService.findOptionAssure("samtoolsCommand", null, null)
        return "cd ${path}; ${samToolsBinary} index ${fileName}; chmod 440 ${baiFilePath}"
    }
}
