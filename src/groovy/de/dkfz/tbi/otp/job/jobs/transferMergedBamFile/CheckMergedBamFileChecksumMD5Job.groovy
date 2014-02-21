package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CheckMergedBamFileChecksumMD5Job extends AbstractJobImpl {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ChecksumFileService checksumFileService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile file = ProcessedMergedBamFile.get(id)
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(file)
        String temporalDestinationDir = locations.get("temporalDestinationDir")
        Project project = processedMergedBamFileService.project(file)
        Realm realm = configService.getRealmDataManagement(project)

        log.debug "Attempting to check copied merged BAM file " + locations.get("bamFile") + " (id= " + file + ")"
        String jobId = executionHelperService.sendScript(realm) { """
cd ${temporalDestinationDir}
md5sum -c ${locations.md5BamFile}
md5sum -c ${locations.md5BaiFile}
""" }
        log.debug "Job ${jobId} submitted to PBS"
    }
}
