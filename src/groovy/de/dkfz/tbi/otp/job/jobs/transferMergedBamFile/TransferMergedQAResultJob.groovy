package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ProcessStatusService
import de.dkfz.tbi.otp.ngsdata.*

class TransferMergedQAResultJob extends AbstractEndStateAwareJobImpl{

    final String JOB = "__pbsIds"
    final String REALM = "__pbsRealm"

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ConfigService configService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessStatusService processStatusService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.get(id)
        Project project = processedMergedBamFileService.project(bamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(bamFile)
        String dirToLog = processStatusService.statusLogFile(temporalDestinationDir)
        Map<String, String> clusterPrefix = configService.clusterSpecificCommandPrefixes(project)

        if (processStatusService.statusSuccessful(dirToLog, CheckMergedBamFileChecksumMD5Job.class.name)) {
            String cmd = scriptText(bamFile, dirToLog, clusterPrefix)
            Realm realm = configService.getRealmDataProcessing(project)
            String jobId = executionHelperService.sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            addOutputParameter(JOB, jobId)
            addOutputParameter(REALM, realm.id.toString())
            succeed()
        } else {
            log.debug "the job ${CheckMergedBamFileChecksumMD5Job.class.name} failed"
            fail()
        }
    }

    private String scriptText(ProcessedMergedBamFile file, String dirToLog, Map<String, String> clusterPrefix) {
        String tmpQADestinationDirectory = processedMergedBamFileService.qaResultTempDestinationDirectory(file)
        QualityAssessmentMergedPass pass = qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(file)
        String sourceQAResultDirectory = processedMergedBamFileQaFileService.directoryPath(pass)
        String text = """
set -e

${clusterPrefix.exec} \"mkdir -p -m 0750 ${tmpQADestinationDirectory}\"
${clusterPrefix.cp} -r ${sourceQAResultDirectory}/* ${clusterPrefix.dest}${tmpQADestinationDirectory}
${clusterPrefix.exec} \"find ${tmpQADestinationDirectory} -type f -exec chmod 0640 '{}' \\;\"
"""
        text += "${clusterPrefix.exec} \"echo ${this.class.name} >> ${dirToLog} ; chmod 0644 ${dirToLog}\""
        return text
    }
}
