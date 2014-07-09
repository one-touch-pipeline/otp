package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filehandling.FileNames
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ProcessStatusService
import de.dkfz.tbi.otp.ngsdata.*

class CreateQAResultStatisticsFileJob extends AbstractEndStateAwareJobImpl {

    final String JOB = "__pbsIds"
    final String REALM = "__pbsRealm"

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    QAResultStatisticsService qaResultStatisticsService

    @Autowired
    ProcessStatusService processStatusService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)

        String dest = processedMergedBamFileService.destinationDirectory(mergedBamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)

        String dirToLog = processStatusService.statusLogFile(temporalDestinationDir)
        if (processStatusService.statusSuccessful(dirToLog, CheckQaResultsChecksumMD5Job.class.name)) {
            Map<String, String> results = qaResultStatisticsService.defineOutput(mergedBamFile)
            Map<String, String> statisticsFiles = qaResultStatisticsService.statisticsFile(mergedBamFile)
            List<DataFile> fastqFiles = processedMergedBamFileService.fastqFilesPerMergedBamFile(mergedBamFile)
            log.debug "Attempting to create statistics result file"
            Project project = processedMergedBamFileService.project(mergedBamFile)
            String cmd = scriptText(temporalDestinationDir, results, statisticsFiles, dirToLog, fastqFiles, mergedBamFile)
            Realm realm = configService.getRealmDataManagement(project)
            String jobId = executionHelperService.sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            addOutputParameter(JOB, jobId)
            addOutputParameter(REALM, realm.id.toString())
            succeed()
        } else {
            addOutputParameter(JOB, "")
            addOutputParameter(REALM, "")
            log.debug "the job ${CheckQaResultsChecksumMD5Job.class.name} failed"
            fail()
        }
    }

    private String scriptText(String temporalDestinationDir, Map<String, String> results, Map<String, String> statisticsFiles, String dirToLog, List<DataFile> fastqFiles, ProcessedMergedBamFile mergedBamFile) {
        // FIXME: This is an ugly hack which should be fixed properly when OTP-504 is resolved
        // FIXME: remove chmod once the ACLs in the file system are in place
        List<String> fastqFileNamesList = fastqFiles*.fileName
        String fastqFilesNames = fastqFileNamesList.join("\n")

        // for the case when the kit of one of the lanes, merged in the mergedBamFile, was inferred, the inferred kit will be returned, otherwise null
        ExomeEnrichmentKit inferredKit = processedMergedBamFileService.getInferredKit(mergedBamFile)
        String inferredMessage = ""
        if (inferredKit) {
            inferredMessage = """
The exome enrichment kit was not available for all lanes in the corresponding mergedBamFile.
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
        text += "echo ${this.class.name} >> ${dirToLog} ; chmod 0644 ${dirToLog}"
        return text
    }
}
