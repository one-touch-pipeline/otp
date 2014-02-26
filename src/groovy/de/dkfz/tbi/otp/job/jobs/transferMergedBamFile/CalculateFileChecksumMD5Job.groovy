package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile


import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CalculateFileChecksumMD5Job extends AbstractJobImpl {

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ConfigService configService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    JobStatusLoggingService jobStatusLoggingService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.get(id)

        // Work-around for bug OTP-397:
        processedMergedBamFileService.updateFileOperationStatus(bamFile, AbstractBamFile.FileOperationStatus.INPROGRESS)
        // End of work-around

        Project project = processedMergedBamFileService.project(bamFile)
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(bamFile)
        Map<String, String> clusterPrefix = configService.clusterSpecificCommandPrefixes(project)
        Realm realm = configService.getRealmForDKFZLSDF(Realm.OperationType.DATA_PROCESSING)
        String projectDir = configService.getProjectRootPath(project) + "/" + project.dirName
        String logDir  = jobStatusLoggingService.logFileBaseDir(realm, processingStep)
        String logFile = jobStatusLoggingService.logFileLocation(realm, processingStep)
        QualityAssessmentMergedPass pass = qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(bamFile)
        String qaResultDirectory = processedMergedBamFileQaFileService.directoryPath(pass)
        String qaResultMd5sumFile = processedMergedBamFileQaFileService.qaResultsMd5sumFile(bamFile)
        Map<String, String> singleLaneQAResultsDirectories = processedMergedBamFileService.singleLaneQAResultsDirectories(bamFile)

        String jobId = executionHelperService.sendScript(realm) {
            // FIXME: remove chmod once the ACLs in the file system are in place
            // the md5sum of the merged bam file is written to the file md5BamFile
            // the md5sum of the bai file is written to the file md5BaiFile
            // the md5sums of the qa results for the merged bam file and for the single lane bam files are written to the file "MD5SUMS"
            String text = """
# Clean-up old log file
mkdir -p -m 2750 '${logDir}'; rm -f '${logFile}'

${clusterPrefix.exec} \"mkdir -p -m 2750 ${locations.temporalDestinationDir}; find ${projectDir} -user \${USER} -type d -not -perm 2750 -exec chmod 2750 '{}' \\;\"

cd ${locations.sourceDirectory}
md5sum ${locations.bamFile} > ${locations.md5BamFile}
chmod 0640 ${locations.md5BamFile}
md5sum ${locations.baiFile} > ${locations.md5BaiFile}
chmod 0640 ${locations.md5BaiFile}
cd ${qaResultDirectory}
rm -f ${qaResultMd5sumFile}
find . -type f -a -not -name ${processedMergedBamFileQaFileService.MD5SUM_NAME} -exec md5sum '{}' \\; >> ${qaResultMd5sumFile}
"""
            // since the full source path of the qa results for the single lane bam files is stored in the MD5SUMS file,
            // we replace them with the relative path in the destination directory
            for (String singleLaneDestinationDir : singleLaneQAResultsDirectories.keySet()) {
                String singleLaneSourceDir = singleLaneQAResultsDirectories.get(singleLaneDestinationDir)
                text += """find ${singleLaneSourceDir} -type f -exec md5sum '{}' \\; >> ${qaResultMd5sumFile}
sed -i 's,${singleLaneSourceDir},./${singleLaneDestinationDir},' ${qaResultMd5sumFile}
"""
            }
            text += "chmod 0640 ${qaResultMd5sumFile}"
            return text
        }
        log.debug "Job " + jobId + " submitted to PBS"
    }
}
