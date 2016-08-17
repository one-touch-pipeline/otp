package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class CheckMergedBamFileChecksumMD5Job extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

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
        String jobId = clusterJobSchedulerService.executeJob(realm, cmd)
        log.debug "Job ${jobId} submitted to cluster job scheduler"

        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
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
