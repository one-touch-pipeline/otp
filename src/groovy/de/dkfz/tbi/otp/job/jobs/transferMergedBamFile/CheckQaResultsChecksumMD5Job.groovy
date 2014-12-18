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
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile file = ProcessedMergedBamFile.get(id)
        String temporalqaDestinationDir = processedMergedBamFileService.qaResultTempDestinationDirectory(file)

        log.debug "Attempting to check copied qa results"
        Project project = processedMergedBamFileService.project(file)
        String cmd = scriptText(temporalqaDestinationDir)
        Realm realm = configService.getRealmDataManagement(project)
        String jobId = executionHelperService.sendScript(realm, cmd)
        log.debug "Job ${jobId} submitted to PBS"

        addOutputParameter(JOB, jobId)
        addOutputParameter(REALM, realm.id.toString())
        succeed()
    }

    private String scriptText(String temporalqaDestinationDir) {

        String text = """
cd ${temporalqaDestinationDir}
md5sum -c ${processedMergedBamFileQaFileService.MD5SUM_NAME}
"""
        return text
    }
}

