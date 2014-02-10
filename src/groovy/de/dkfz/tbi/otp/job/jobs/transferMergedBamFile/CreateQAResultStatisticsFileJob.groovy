package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filehandling.FileNames
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CreateQAResultStatisticsFileJob extends AbstractJobImpl {

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
    QAResultStatisticsService qaResultStatisticsService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)
        String dest = processedMergedBamFileService.destinationDirectory(mergedBamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        Map<String, String> results = qaResultStatisticsService.defineOutput(mergedBamFile)
        Map<String, String> statisticsFiles = qaResultStatisticsService.statisticsFile(mergedBamFile)
        List<DataFile> fastqFiles = processedMergedBamFileService.fastqFilesPerMergedBamFile(mergedBamFile)
        List<String> fastqFileNamesList = fastqFiles*.fileName
        String fastqFilesNames = fastqFileNamesList.join("\n")
        log.debug "Attempting to create statistics result file"
        Project project = processedMergedBamFileService.project(mergedBamFile)
        Realm realm = configService.getRealmDataManagement(project)
        String jobId = executionHelperService.sendScript(realm) { // FIXME: This is an ugly hack which should be fixed properly when OTP-504 is resolved
            // FIXME: remove chmod once the ACLs in the file system are in place
            """
cd ${temporalDestinationDir}
cat <<EOD > ${statisticsFiles.small}
${results.small}
EOD
cat <<EOD > ${statisticsFiles.extended}
${results.extended}
EOD
cat <<EOD > ${FileNames.FASTQ_FILES_IN_MERGEDBAMFILE}
${fastqFilesNames}
EOD
chmod 0440 ${statisticsFiles.small} ${statisticsFiles.extended} ${FileNames.FASTQ_FILES_IN_MERGEDBAMFILE}
""" }
        log.debug "Job ${jobId} submitted to PBS"
    }
}
