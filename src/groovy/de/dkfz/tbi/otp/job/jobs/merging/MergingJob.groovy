package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class MergingJob extends AbstractJobImpl {

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ConfigService configService

    @Autowired
    MergingPassService mergingPassService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    MergingSetAssignmentService mergingSetAssignmentService

    @Autowired
    ProcessingOptionService optionService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)
        Realm realm = mergingPassService.realmForDataProcessing(mergingPass)
        String cmd = createCommand(alignmentPass)
        log.debug cmd
        String pbsId = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsId)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String createCommand(ProcessedMergedBamFile mergedBamFile) {
        String baseDir = processedMergedBamFileService.getDirectory(mergedBamFile)
        String tempDir = "${baseDir}/tmp_picard"
        String createTempDir = "mkdir ${tempDir}"
        String javaOptions = optionService.findOptionSafe("picardJavaSetting", null, null)
        String picard = "picard.sh MarkDuplicates"
        String inputFilePath = createInputFileString(mergedBamFile)
        String outputFilePath = processedMergedBamFileService.getFilePath(mergedBamFile)
        String outputFileNoSuffix = processedMergedBamFileService.getFilePathNoSuffix(mergedBamFile)
        String metricsPath = processedMergedBamFileService.getFilePathForMetrics(mergedBamFile)
        String picardFiles = "${inputFilePath} OUTPUT=${outputFilePath} METRICS_FILE=${metricsPath} TMP_DIR=${tempDir}"
        String picardOptions = optionService.findOptionSafe("picardRmdup", null, null)
        String chmod = "chmod 440 ${outputFilePath} ${metricsPath}"
        return "${createTempDir}; ${javaOptions}; ${picard} ${picardFiles} ${picardOptions}; ${chmod}"
    }

    private String createInputFileString(ProcessedMergedBamFile processedMergedBamFile) {
        StringBuilder ret = new StringBuilder();
        mergingSetAssignmentService.findByMergingSet(processedMergedBamFile.mergingPass.mergingSet).each {
            String fileName = processedBamFileService.getFilePath(it.bamFile)
            ret.add(" I=${fileName}")
        }
        return ret.toString()
    }

}
