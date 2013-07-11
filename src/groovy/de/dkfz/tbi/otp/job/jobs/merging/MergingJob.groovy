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
    ProcessedBamFileService processedBamFileService

    @Autowired
    MergingSetAssignmentService mergingSetAssignmentService

    @Autowired
    ProcessingOptionService optionService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)
        Realm realm = mergingPassService.realmForDataProcessing(mergingPass)
        String cmd = createCommand(processedMergedBamFile)
        log.debug cmd
        String pbsId = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsId)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String createCommand(ProcessedMergedBamFile processedMergedBamFile) {
        String baseDir = processedMergedBamFileService.getDirectory(processedMergedBamFile)
        String tempDir = "${baseDir}/tmp_picard"
        String createTempDir = "mkdir ${tempDir}"
        String javaOptions = optionService.findOptionSafe("picardJavaSetting", null, null)
        String picard = "picard.sh MarkDuplicates"
        String inputFilePath = createInputFileString(processedMergedBamFile)
        String outputFilePath = processedMergedBamFileService.getFilePath(processedMergedBamFile)
        String metricsPath = processedMergedBamFileService.getFilePathForMetrics(processedMergedBamFile)
        String picardFiles = "${inputFilePath} OUTPUT=${outputFilePath} METRICS_FILE=${metricsPath} TMP_DIR=${tempDir}"
        String picardOptions = optionService.findOptionSafe("picardRmdup", null, null)
        String chmod = "chmod 440 ${outputFilePath} ${metricsPath}"
        return "${createTempDir}; ${javaOptions}; ${picard} ${picardFiles} ${picardOptions}; ${chmod}"
    }

    private String createInputFileString(ProcessedMergedBamFile processedMergedBamFile) {
        StringBuilder stringBuilder = new StringBuilder()
        MergingSetAssignment.findAllByMergingSet(processedMergedBamFile.mergingPass.mergingSet).each {
            String fileName = processedBamFileService.getFilePath(it.bamFile)
            stringBuilder.append(" I=${fileName}")
        }
        return stringBuilder.toString()
    }
}
