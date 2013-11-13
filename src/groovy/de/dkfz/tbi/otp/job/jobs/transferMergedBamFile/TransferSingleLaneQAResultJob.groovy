package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ProcessStatusService
import de.dkfz.tbi.otp.ngsdata.*

class TransferSingleLaneQAResultJob extends AbstractEndStateAwareJobImpl{

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
    ProcessedBamFileService processedBamFileService

    @Autowired
    ProcessStatusService processStatusService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)
        Project project = processedMergedBamFileService.project(mergedBamFile)
        Map<String, String> singleLaneQAResultsDirectories = processedMergedBamFileService.singleLaneQAResultsDirectories(mergedBamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        String dirToLog = processStatusService.statusLogFile(temporalDestinationDir)
        Map<String, String> clusterPrefix = configService.clusterSpecificCommandPrefixes(project)
        if (processStatusService.statusSuccessful(dirToLog, TransferMergedQAResultJob.class.name)) {
            String cmd = scriptText(mergedBamFile, singleLaneQAResultsDirectories, dirToLog, clusterPrefix)
            Realm realm = configService.getRealmDataProcessing(project)
            String jobId = executionHelperService.sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            addOutputParameter(JOB, jobId)
            addOutputParameter(REALM, realm.id.toString())
            succeed()
        } else {
            log.debug "the job ${TransferMergedQAResultJob.class.name} failed"
            fail()
        }
    }

    private String scriptText(ProcessedMergedBamFile file, Map<String, String> directories, String dirToLog, Map<String, String> clusterPrefix) {
        String tmpQADestinationDirectory = processedMergedBamFileService.qaResultTempDestinationDirectory(file)
        String qaDestinationDirectory = processedMergedBamFileService.qaResultDestinationDirectory(file)
        String text = """
set -e

"""

        for (String directoryName : directories.keySet()) {
            String src = directories.get(directoryName)
            text += """
# Remove old QA results directory for single lane BAM files if they exist
${clusterPrefix.exec} \"rm -r -f ${qaDestinationDirectory}/${directoryName}\"
${clusterPrefix.exec} \"mkdir -p -m 0750 ${tmpQADestinationDirectory}/${directoryName}\"
${clusterPrefix.cp} -r ${src}/* ${clusterPrefix.dest}${tmpQADestinationDirectory}/${directoryName}
${clusterPrefix.exec} \"find ${tmpQADestinationDirectory}/${directoryName} -type f -exec chmod 0640 '{}' \\;\"
"""
        }
        text += "${clusterPrefix.exec} \"echo ${this.class.name} >> ${dirToLog} ; chmod 0644 ${dirToLog}\""
        return text
    }
}
