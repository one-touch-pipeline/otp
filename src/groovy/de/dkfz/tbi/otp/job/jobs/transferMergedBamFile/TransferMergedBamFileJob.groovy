package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import de.dkfz.tbi.otp.config.*
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
class TransferMergedBamFileJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ChecksumFileService checksumFileService

    @Override
    void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile file = ProcessedMergedBamFile.get(id)
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(file)
        String temporalDestinationDir = locations.get("temporalDestinationDir")

        log.debug "Attempting to copy merged BAM file " + locations.get("bamFile") + " (id= " + file + " ) to " + locations.get("destinationDirectory")
        Project project = processedMergedBamFileService.project(file)
        String cmd = scriptText(locations, temporalDestinationDir)
        Realm realm = project.realm
        String jobId = clusterJobSchedulerService.executeJob(realm, cmd)
        log.debug "Job ${jobId} submitted to cluster job scheduler"

        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
        succeed()
    }

    private String scriptText(Map<String, String> locations, String temporalDestinationDir) {
        String source = locations.get("sourceDirectory")

        // FIXME: remove chmod once the ACLs in the file system are in place
        String text = """
cd ${source}
cp *.bam *.bai *.md5sum ${temporalDestinationDir}
find ${temporalDestinationDir} -type f -exec chmod 0644 '{}' \\;
"""
        return text
    }
}
