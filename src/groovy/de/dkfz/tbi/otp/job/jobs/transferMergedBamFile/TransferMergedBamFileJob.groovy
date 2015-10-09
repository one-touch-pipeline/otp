package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.ProcessStatusService
import de.dkfz.tbi.otp.ngsdata.*

class TransferMergedBamFileJob extends AbstractEndStateAwareJobImpl {

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

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile file = ProcessedMergedBamFile.get(id)
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(file)
        String temporalDestinationDir = locations.get("temporalDestinationDir")

        log.debug "Attempting to copy merged BAM file " + locations.get("bamFile") + " (id= " + file + " ) to " + locations.get("destinationDirectory")
        Project project = processedMergedBamFileService.project(file)
        String cmd = scriptText(locations, temporalDestinationDir)
        Realm realm = configService.getRealmDataProcessing(project)
        String jobId = executionHelperService.sendScript(realm, cmd)
        log.debug "Job ${jobId} submitted to PBS"

        addOutputParameter(JOB, jobId)
        addOutputParameter(REALM, realm.id.toString())
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
