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
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    AbstractBamFileService abstractBamFileService

    @Autowired
    ProcessingOptionService optionService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile.withTransaction {
            ProcessedMergedBamFile processedMergedBamFile = processedMergedBamFileService.createMergedBamFile(mergingPass)
            Realm realm = mergingPassService.realmForDataProcessing(mergingPass)
            String cmd = createCommand(processedMergedBamFile)
            String pbsId = executionHelperService.sendScript(realm, cmd, "mergingJob")

            addOutputParameter("__pbsIds", pbsId)
            addOutputParameter("__pbsRealm", realm.id.toString())
        }
    }

    private String createCommand(ProcessedMergedBamFile processedMergedBamFile) {
        Project project = mergingPassService.project(processedMergedBamFile.mergingPass)
        String tempDir = "\${PBS_SCRATCH_DIR}/\${PBS_JOBID}"
        String createTempDir = "mkdir -p -m 2750 ${tempDir}"
        String javaOptions = optionService.findOptionSafe("picardJavaSetting", null, project)
        String picard = "picard.sh MarkDuplicates"
        String inputFilePath = createInputFileString(processedMergedBamFile)
        String outputFilePath = processedMergedBamFileService.filePath(processedMergedBamFile)
        String metricsPath = processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
        String baiFilePath = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        String picardFiles = "${inputFilePath} OUTPUT=${outputFilePath} METRICS_FILE=${metricsPath} TMP_DIR=${tempDir}"
        String picardOptions = optionService.findOptionSafe("picardMdup", null, project)
        String chmod = "chmod 440 ${outputFilePath} ${metricsPath} ${baiFilePath}"
        return "${createTempDir}; ${javaOptions}; ${picard} ${picardFiles} ${picardOptions}; ${chmod}"
    }

    private String createInputFileString(ProcessedMergedBamFile processedMergedBamFile) {
        List<AbstractBamFile> abstractBamFiles = abstractBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        notEmpty(abstractBamFiles, "No BamFiles found for ${processedMergedBamFile}")
        if (abstractBamFiles.size() == 1) {
            if (abstractBamFiles.get(0) instanceof ProcessedMergedBamFile) {
                throw new RuntimeException("Merging set ${processedMergedBamFile.mergingSet} contains a single merged BAM file only. This makes no sense.")
            }
        }
        StringBuilder stringBuilder = new StringBuilder()
        abstractBamFiles.each { AbstractBamFile abstractBamFile ->
            String fileName
            if (abstractBamFile instanceof ProcessedBamFile) {
                fileName = processedBamFileService.getFilePath(abstractBamFile)
            } else if (abstractBamFile instanceof ProcessedMergedBamFile) {
                fileName = processedMergedBamFileService.filePath(abstractBamFile)
            } else {
                throw new RuntimeException("service for class ${abstractBamFile.class} is unknown")
            }
            stringBuilder.append(" I=${fileName}")
        }
        return stringBuilder.toString()
    }
}
