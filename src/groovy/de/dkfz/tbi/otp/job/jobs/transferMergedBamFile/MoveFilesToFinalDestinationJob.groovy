package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filehandling.*
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
class MoveFilesToFinalDestinationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    ChecksumFileService checksumFileService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)
        String dest = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        String temporalQADestinationDir = processedMergedBamFileService.qaResultTempDestinationDirectory(mergedBamFile)
        String qaDestinationDirectory = processedMergedBamFileService.qaResultDestinationDirectory(mergedBamFile)
        Project project = processedMergedBamFileService.project(mergedBamFile)
        //has to be changed since the location of the log file moved from the .tmp folder to the final destination
        Realm realm = configService.getRealmDataManagement(project)
        String cmd = scriptText(dest, temporalDestinationDir, temporalQADestinationDir, qaDestinationDirectory)
        mergedBamFile.validateAndSetBamFileInProjectFolder()
        log.debug "Attempting to move files from the tmp directory to the final destination"
        String jobId = clusterJobSchedulerService.executeJob(realm, cmd)
        log.debug "Job ${jobId} submitted to cluster job scheduler"

        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
        succeed()
    }

    //before moving the files to the final directory it is checked if the files, which are currently at the destination, are in use
    private String scriptText(String dest, String temporalDestinationDir, String temporalQADestinationDir, String qaDestinationDirectory) {
        String text = """
mkdir -p -m 2750 ${dest}${processedMergedBamFileService.QUALITY_ASSESSMENT_DIR}
mv -f ${temporalDestinationDir}/*.bam ${temporalDestinationDir}/*.bai ${temporalDestinationDir}/*.md5sum ${temporalDestinationDir}/${FileNames.FASTQ_FILES_IN_MERGEDBAMFILE} ${dest}
mv -f ${temporalQADestinationDir}/* ${qaDestinationDirectory}
rm -rf ${temporalDestinationDir}
"""
        // Work-around for OTP-1018: Add read permissions to BAM and BAI files to work around a bug
        // in the CREST tool. The files are still protected by the permissions of the parent directory.
        text += "chmod o+r ${dest}/*.bam ${dest}/*.bai\n"
        // End of work-around

        return text
    }
}
