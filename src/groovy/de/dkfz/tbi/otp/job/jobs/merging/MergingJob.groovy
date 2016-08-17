package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import static org.springframework.util.Assert.*

@Component
@Scope("prototype")
@UseJobLog
class MergingJob extends AbstractJobImpl {

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

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
            String cmd = createCommand(processedMergedBamFile, realm)
            String jobId = clusterJobSchedulerService.executeJob(realm, cmd)

            addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
            addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
        }
    }

    private String createCommand(ProcessedMergedBamFile processedMergedBamFile, Realm realm) {
        Project project = mergingPassService.project(processedMergedBamFile.mergingPass)
        String tempDir = "\${PBS_SCRATCH_DIR}/${ClusterJobSchedulerService.getJobIdEnvironmentVariable(realm)}"
        String createTempDir = "mkdir -p -m 2750 ${tempDir}"
        String javaOptions = optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_PICARD_JAVA_SETTINGS, null, project)
        String picard = ProcessingOptionService.findOptionAssure(OptionName.COMMAND_PICARD_MDUP, null, project)
        String inputFilePath = createInputFileString(processedMergedBamFile)
        String outputFilePath = processedMergedBamFileService.filePath(processedMergedBamFile)
        String metricsPath = processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
        String baiFilePath = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        String picardFiles = "${inputFilePath} OUTPUT=${outputFilePath} METRICS_FILE=${metricsPath} TMP_DIR=${tempDir}"
        String picardOptions = optionService.findOptionSafe(OptionName.PIPELINE_OTP_ALIGNMENT_PICARD_MDUP, null, project)
        String chmod = "chmod 440 ${outputFilePath} ${metricsPath} ${baiFilePath}"
        return "${createTempDir}; ${javaOptions}; ${picard} ${picardFiles} ${picardOptions}; ${chmod}"
    }

    public String createInputFileString(ProcessedMergedBamFile processedMergedBamFile) {
        List<AbstractBamFile> abstractBamFiles = abstractBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        notEmpty(abstractBamFiles, "No BamFiles found for ${processedMergedBamFile}")
        if (abstractBamFiles.size() == 1) {
            if (abstractBamFiles.get(0) instanceof ProcessedMergedBamFile) {
                throw new RuntimeException("Merging set ${processedMergedBamFile.mergingSet} contains a single merged BAM file only. This makes no sense.")
            }
        }
        StringBuilder stringBuilder = new StringBuilder()
        abstractBamFiles.each { AbstractBamFile abstractBamFile ->
            File file
            if (abstractBamFile instanceof ProcessedBamFile) {
                file = new File(processedBamFileService.getFilePath(abstractBamFile))
            } else if (abstractBamFile instanceof ProcessedMergedBamFile) {
                file = new File(processedMergedBamFileService.filePath(abstractBamFile))
            } else {
                throw new RuntimeException("service for class ${abstractBamFile.class} is unknown")
            }
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
            assert abstractBamFile.fileSize == file.length()
            stringBuilder.append(" I=${file}")
        }
        return stringBuilder.toString()
    }
}
