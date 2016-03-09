package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CheckMergedBamFileChecksumMD5Job extends AbstractEndStateAwareJobImpl {

    final String JOB = "__pbsIds"
    final String REALM = "__pbsRealm"

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    PbsService pbsService

    @Autowired
    ChecksumFileService checksumFileService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile file = ProcessedMergedBamFile.get(id)
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(file)
        String temporalDestinationDir = locations.get("temporalDestinationDir")

        log.debug "Attempting to check copied merged BAM file " + locations.get("bamFile") + " (id= " + file + ")"
        Project project = processedMergedBamFileService.project(file)
        String cmd = scriptText(locations, temporalDestinationDir)
        Realm realm = configService.getRealmDataManagement(project)
        String jobId = pbsService.executeJob(realm, cmd)
        log.debug "Job ${jobId} submitted to PBS"

        addOutputParameter(JOB, jobId)
        addOutputParameter(REALM, realm.id.toString())
        succeed()
    }

    private String scriptText(Map<String, String> locations, String temporalDestinationDir) {
        String md5Bam = locations.get("md5BamFile")
        String md5Bai = locations.get("md5BaiFile")

        String text = """
cd ${temporalDestinationDir}
md5sum -c ${md5Bam}
md5sum -c ${md5Bai}
"""
        return text
    }
}
