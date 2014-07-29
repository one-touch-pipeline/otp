package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filehandling.FileNames
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.scheduler.ProcessStatusService
import de.dkfz.tbi.otp.ngsdata.*

class MoveFilesToFinalDestinationJob extends AbstractEndStateAwareJobImpl {

    final String JOB = "__pbsIds"
    final String REALM = "__pbsRealm"

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    QAResultStatisticsService qaResultFromDatabaseService

    @Autowired
    ProcessStatusService processStatusService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)
        String dest = processedMergedBamFileService.destinationDirectory(mergedBamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        String temporalQADestinationDir = processedMergedBamFileService.qaResultTempDestinationDirectory(mergedBamFile)
        String qaDestinationDirectory = processedMergedBamFileService.qaResultDestinationDirectory(mergedBamFile)

        String dirToLog = processStatusService.statusLogFile(temporalDestinationDir)
        if (processStatusService.statusSuccessful(dirToLog, CreateQAResultStatisticsFileJob.class.name)) {
            log.debug "Attempting to move files from the tmp directory to the final destination"
            Project project = processedMergedBamFileService.project(mergedBamFile)
            //has to be changed since the location of the log file moved from the .tmp folder to the final destination
            dirToLog = processStatusService.statusLogFile(dest)
            Realm realm = configService.getRealmDataManagement(project)
            String projectDir = realm.rootPath + "/" + project.dirName
            String cmd = scriptText(dest, temporalDestinationDir, dirToLog, projectDir, temporalQADestinationDir, qaDestinationDirectory, processedMergedBamFileService.inProgressFileName(mergedBamFile))
            String jobId = executionHelperService.sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            addOutputParameter(JOB, jobId)
            addOutputParameter(REALM, realm.id.toString())
            succeed()
        } else {
            addOutputParameter(JOB, "")
            addOutputParameter(REALM, "")
            log.debug "the job ${CreateQAResultStatisticsFileJob.class.name} failed"
            fail()
        }
    }

    //before moving the files to the final directory it is checked if the files, which are currently at the destination, are in use
    private String scriptText(String dest, String temporalDestinationDir, String dirToLog, String projectDir, String temporalQADestinationDir, String qaDestinationDirectory, String inProgressFileName) {
        String text = """
mkdir -p -m 2750 ${dest}${processedMergedBamFileService.QUALITY_ASSESSMENT_DIR}
flock -x ${dest} -c \"mv -f ${temporalDestinationDir}/*.bam ${temporalDestinationDir}/*.bai ${temporalDestinationDir}/*.md5sum ${temporalDestinationDir}/*.log ${temporalDestinationDir}/${FileNames.FASTQ_FILES_IN_MERGEDBAMFILE} ${dest}\"
flock -x ${dest} -c \"mv -f ${temporalQADestinationDir}/* ${qaDestinationDirectory}/\"
rm -rf ${temporalDestinationDir}
rm -f ${dest}/${inProgressFileName}
"""
        // Work-around for OTP-1018: Add read permissions to BAM and BAI files to work around a bug
        // in the CREST tool. The files are still protected by the permissions of the parent directory.
        text += "chmod o+r ${dest}/*.bam ${dest}/*.bai\n"
        // End of work-around

        text += "echo ${this.class.name} >> ${dirToLog} ; chmod 0644 ${dirToLog}"
        return text
    }
}
