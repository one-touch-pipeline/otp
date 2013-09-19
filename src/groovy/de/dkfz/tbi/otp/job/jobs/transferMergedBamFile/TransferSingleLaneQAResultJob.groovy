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
        if (processStatusService.statusSuccessful(dirToLog, TransferMergedQAResultJob.class.name)) {
            String cmd = scriptText(mergedBamFile, singleLaneQAResultsDirectories, dirToLog)
            Realm realm = configService.getRealmDataManagement(project)
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

    private String scriptText(ProcessedMergedBamFile file, Map<String, String> directories, String dirToLog) {
        String tmpQADestinationDirectory = processedMergedBamFileService.qaResultTempDestinationDirectory(file)
        String text = """
set -e

cd ${tmpQADestinationDirectory}
"""

        for (String directoryName : directories.keySet()) {
            String src = directories.get(directoryName)
            text += """
mkdir -p -m 0750 ${directoryName}
cp -r ${src}/* ${directoryName}
find ${directoryName} -type f -exec chmod 0640 '{}' \\;
"""
        }
        text += "echo ${this.class.name} >> ${dirToLog} ; chmod 0644 ${dirToLog}"
        return text
    }
}
