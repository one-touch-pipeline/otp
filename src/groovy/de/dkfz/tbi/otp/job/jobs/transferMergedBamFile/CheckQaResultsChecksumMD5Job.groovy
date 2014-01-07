package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CheckQaResultsChecksumMD5Job extends AbstractJobImpl {

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
        String qaResultMd5sumFile = processedMergedBamFileQaFileService.qaResultsMd5sumFile(file)
        String temporalqaDestinationDir = processedMergedBamFileService.qaResultTempDestinationDirectory(file)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(file)
        log.debug "Attempting to check copied qa results"
        Project project = processedMergedBamFileService.project(file)
        Realm realm = configService.getRealmDataManagement(project)
        String jobId = executionHelperService.sendScript(realm) { """
cd ${temporalqaDestinationDir}
md5sum -c ${processedMergedBamFileQaFileService.MD5SUM_NAME}
""" }
        log.debug "Job ${jobId} submitted to PBS"
    }
}
