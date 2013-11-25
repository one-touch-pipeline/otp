package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
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
            log.debug "Attempting to create statistics result file"
            Project project = processedMergedBamFileService.project(mergedBamFile)
            String cmd = scriptText(temporalDestinationDir, results, statisticsFiles, dirToLog)
            Realm realm = configService.getRealmDataManagement(project)
            String jobId = executionHelperService.sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            addOutputParameter(JOB, jobId)
            addOutputParameter(REALM, realm.id.toString())
            succeed()
        } else {
            log.debug "the job ${CheckQaResultsChecksumMD5Job.class.name} failed"
            fail()
        }
    }

    private String scriptText(String temporalDestinationDir, Map<String, String> results, Map<String, String> statisticsFiles, String dirToLog) {
        // FIXME: This is an ugly hack which should be fixed properly when OTP-504 is resolved
        // FIXME: remove chmod once the ACLs in the file system are in place
        String text = """
cd ${temporalDestinationDir}
cat <<EOD > ${statisticsFiles.small}
${results.small}
EOD
cat <<EOD > ${statisticsFiles.extended}
${results.extended}
EOD
chmod 0440 ${statisticsFiles.small} ${statisticsFiles.extended}
"""
        text += "echo ${this.class.name} >> ${dirToLog} ; chmod 0644 ${dirToLog}"
        return text
    }
}
