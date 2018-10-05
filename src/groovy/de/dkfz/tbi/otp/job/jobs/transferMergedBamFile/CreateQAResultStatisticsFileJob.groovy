package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filehandling.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class CreateQAResultStatisticsFileJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    QAResultStatisticsService qaResultStatisticsService

    @Override
    void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)

        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)

        Map<String, String> results = qaResultStatisticsService.defineOutput(mergedBamFile)
        Map<String, String> statisticsFiles = qaResultStatisticsService.statisticsFile(mergedBamFile)
        List<DataFile> fastqFiles = processedMergedBamFileService.fastqFilesPerMergedBamFile(mergedBamFile)
        log.debug "Attempting to create statistics result file"
        Project project = processedMergedBamFileService.project(mergedBamFile)
        String cmd = scriptText(temporalDestinationDir, results, statisticsFiles, fastqFiles, mergedBamFile)
        Realm realm = project.realm
        String jobId = clusterJobSchedulerService.executeJob(realm, cmd)
        log.debug "Job ${jobId} submitted to cluster job scheduler"

        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
        succeed()
    }

    private String scriptText(String temporalDestinationDir, Map<String, String> results, Map<String, String> statisticsFiles, List<DataFile> fastqFiles, ProcessedMergedBamFile mergedBamFile) {
        // FIXME: This is an ugly hack which should be fixed properly when OTP-504 is resolved
        List<String> fastqFileNamesList = fastqFiles*.fileName
        String fastqFilesNames = fastqFileNamesList.join("\n")

        // for the case when the kit of one of the lanes, merged in the mergedBamFile, was inferred, the inferred kit will be returned, otherwise null
        LibraryPreparationKit inferredKit = processedMergedBamFileService.getInferredKit(mergedBamFile)
        String inferredMessage = ""
        if (inferredKit) {
            inferredMessage = """
The library preparation kit was not available for all lanes in the corresponding mergedBamFile.
The kit ${inferredKit} was inferred to be used.\n\n
"""
        }

        String text = """
cd ${temporalDestinationDir}
cat <<EOD > ${statisticsFiles.small}
${inferredMessage}
${results.small}
EOD
cat <<EOD > ${statisticsFiles.extended}
${inferredMessage}
${results.extended}
EOD
cat <<EOD > ${FileNames.FASTQ_FILES_IN_MERGEDBAMFILE}
${fastqFilesNames}
EOD
chmod 0440 ${statisticsFiles.small} ${statisticsFiles.extended} ${FileNames.FASTQ_FILES_IN_MERGEDBAMFILE}
"""
        return text
    }
}
