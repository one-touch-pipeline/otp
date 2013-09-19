package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.scheduler.ProcessStatusService
import de.dkfz.tbi.otp.ngsdata.*

class CheckQaResultsChecksumMD5Job extends AbstractEndStateAwareJobImpl {

    final String JOB = "__pbsIds"
    final String REALM = "__pbsRealm"

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ProcessStatusService processStatusService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile file = ProcessedMergedBamFile.get(id)
        String qaResultMd5sumFile = processedMergedBamFileQaFileService.qaResultsMd5sumFile(file)
        String temporalqaDestinationDir = processedMergedBamFileService.qaResultTempDestinationDirectory(file)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(file)
        String dirToLog = processStatusService.statusLogFile(temporalDestinationDir)
        if (processStatusService.statusSuccessful(dirToLog, TransferSingleLaneQAResultJob.class.name)) {
            log.debug "Attempting to check copied qa results"
            Project project = processedMergedBamFileService.project(file)
            String cmd = scriptText(temporalqaDestinationDir, qaResultMd5sumFile, dirToLog)
            Realm realm = configService.getRealmDataManagement(project)
            String jobId = executionHelperService.sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            addOutputParameter(JOB, jobId)
            addOutputParameter(REALM, realm.id.toString())
            succeed()
        } else {
            log.debug "the job ${TransferSingleLaneQAResultJob.class.name} failed"
            fail()
        }
    }

    private String scriptText(String temporalqaDestinationDir, String qaResultMd5sumFile, String dirToLog) {
        // FIXME: remove chmod once the ACLs in the file system are in place
        String text = """
set -e

cd ${temporalqaDestinationDir}
md5sum -c ${qaResultMd5sumFile}
"""
        text += "echo ${this.class.name} >> ${dirToLog} ; chmod 0644 ${dirToLog}"
        return text
    }
}

