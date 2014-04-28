package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class TransferMergedQAResultJob extends AbstractJobImpl{

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

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.get(id)
        Project project = processedMergedBamFileService.project(bamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(bamFile)
        Map<String, String> clusterPrefix = configService.clusterSpecificCommandPrefixes(project)
        String tmpQADestinationDirectory = processedMergedBamFileService.qaResultTempDestinationDirectory(bamFile)
        QualityAssessmentMergedPass pass = qualityAssessmentMergedPassService.latestQualityAssessmentMergedPass(bamFile)
        String sourceQAResultDirectory = processedMergedBamFileQaFileService.directoryPath(pass)
        Realm realm = configService.getRealmDataProcessing(project)
        executionHelperService.sendScript(realm) { """
${clusterPrefix.exec} \"mkdir -p -m 2750 ${tmpQADestinationDirectory}\"
${clusterPrefix.cp} -r ${sourceQAResultDirectory}/* ${clusterPrefix.dest}${tmpQADestinationDirectory}
${clusterPrefix.exec} \"find ${tmpQADestinationDirectory} -type f -exec chmod 0640 '{}' \\;\"
""" }
    }
}
