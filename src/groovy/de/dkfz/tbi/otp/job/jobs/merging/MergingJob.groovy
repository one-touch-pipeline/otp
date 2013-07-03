package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import static org.springframework.util.Assert.*

class MergingJob extends AbstractJobImpl {

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    MergingPassService mergingPassService

    @Autowired
    ProcessedMergingFileService processedMergingFileService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ProcessingOptionService optionService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile processedMergedBamFile = processedMergedBamFileService.createMergedBamFile(mergingPass)
        Realm realm = mergingPassService.realmForDataProcessing(mergingPass)
        String cmd = createCommand(processedMergedBamFile)
        log.debug cmd
        String pbsId = executionHelperService.sendScript(realm, cmd, "mergingJob")
        addOutputParameter("__pbsIds", pbsId)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String createCommand(ProcessedMergedBamFile processedMergedBamFile) {
        Project project = mergingPassService.project(processedMergedBamFile.mergingPass)
        String baseDir = processedMergingFileService.directory(processedMergedBamFile)
        String tempDir = "${baseDir}/tmp_picard"
        String createTempDir = "mkdir -p ${tempDir}"
        String javaOptions = optionService.findOptionSafe("picardJavaSetting", null, project)
        String picard = "picard.sh MarkDuplicates"
        String inputFilePath = createInputFileString(processedMergedBamFile)
        String outputFilePath = processedMergedBamFileService.filePath(processedMergedBamFile)
        String metricsPath = processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
        String picardFiles = "${inputFilePath} OUTPUT=${outputFilePath} METRICS_FILE=${metricsPath} TMP_DIR=${tempDir}"
        String picardOptions = optionService.findOptionSafe("picardMdup", null, project)
        String chmod = "chmod 440 ${outputFilePath} ${metricsPath}"
        return "${createTempDir}; ${javaOptions}; ${picard} ${picardFiles} ${picardOptions}; ${chmod}"
    }

    private String createInputFileString(ProcessedMergedBamFile processedMergedBamFile) {
        List<ProcessedBamFile> processedBamFiles = processedBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        notEmpty(processedBamFiles, "No ProcessedBamFiles found for ${processedMergedBamFile}")
        StringBuilder stringBuilder = new StringBuilder()
        processedBamFiles.each { ProcessedBamFile processedBamFile ->
            String fileName = processedBamFileService.getFilePath(processedBamFile)
            stringBuilder.append(" I=${fileName}")
        }
        return stringBuilder.toString()
    }
}
