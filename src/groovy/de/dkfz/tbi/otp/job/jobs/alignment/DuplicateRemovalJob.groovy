package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class DuplicateRemovalJob extends AbstractJobImpl {

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
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        ProcessedBamFile bamFile = processedBamFileService.createRmdupBamFile(alignmentPass)
        Realm realm = alignmentPassService.realmForDataProcessing(alignmentPass)
        String cmd = createCommand(alignmentPass)
        log.debug cmd
        String pbsId = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsId)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String createCommand(AlignmentPass alignmentPass) {
        ProcessedBamFile sortedBam = processedBamFileService.findSortedBamFile(alignmentPass)
        ProcessedBamFile rmdupBam = processedBamFileService.findRmdupBamFile(alignmentPass)
        String baseDir = processedBamFileService.getDirectory(sortedBam)
        String tempDir = "${baseDir}/tmp_picard"
        String createTempDir = "mkdir ${tempDir}"
        String javaOptions = optionService.findOptionSafe("picardJavaSetting", null, null)
        String picard = "picard.sh MarkDuplicates"
        String inputFilePath = processedBamFileService.getFilePath(sortedBam)
        String outputFilePath = processedBamFileService.getFilePath(rmdupBam)
        String outputFileNoSuffix = processedBamFileService.getFilePathNoSuffix(rmdupBam)
        String metricsPath = "${outputFileNoSuffix}_metrics.txt"
        String picardFiles = "I=${inputFilePath} OUTPUT=${outputFilePath} METRICS_FILE=${metricsPath} TMP_DIR=${tempDir}"
        String picardOptions = optionService.findOptionSafe("picardRmdup", null, null)
        String chmod = "chmod 440 ${outputFilePath} ${metricsPath}"
        return "${createTempDir}; ${javaOptions}; ${picard} ${picardFiles} ${picardOptions}; ${chmod}"
    }
}
