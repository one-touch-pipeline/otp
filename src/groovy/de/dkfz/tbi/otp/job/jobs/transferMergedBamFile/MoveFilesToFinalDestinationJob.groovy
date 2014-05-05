package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filehandling.FileNames
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class MoveFilesToFinalDestinationJob extends AbstractJobImpl {

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

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)
        String dest = processedMergedBamFileService.destinationDirectory(mergedBamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        String temporalQADestinationDir = processedMergedBamFileService.qaResultTempDestinationDirectory(mergedBamFile)
        String qaDestinationDirectory = processedMergedBamFileService.qaResultDestinationDirectory(mergedBamFile)
        log.debug "Attempting to move files from the tmp directory to the final destination"
        Project project = processedMergedBamFileService.project(mergedBamFile)
        Realm realm = configService.getRealmDataManagement(project)
        String projectDir = realm.rootPath + "/" + project.dirName
        // before moving the files to the final directory it is checked if the files, which are currently at the destination, are in use
        executionHelperService.sendScript(realm) { """
mkdir -p ${dest}${processedMergedBamFileService.QUALITY_ASSESSMENT_DIR}
flock -x ${dest} -c \"mv -f ${temporalDestinationDir}/*.bam ${temporalDestinationDir}/*.bai ${temporalDestinationDir}/*.md5sum ${temporalDestinationDir}/${FileNames.FASTQ_FILES_IN_MERGEDBAMFILE} ${dest} && mv -f ${temporalQADestinationDir}/* ${qaDestinationDirectory}/\"
rm -rf ${temporalDestinationDir}
rm -f ${dest}/${processedMergedBamFileService.inProgressFileName(mergedBamFile)}
""" }
    }
}
