package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class TransferSingleLaneQAResultJob extends AbstractJobImpl{

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ConfigService configService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.get(id)
        Project project = processedMergedBamFileService.project(mergedBamFile)
        Map<String, String> singleLaneQAResultsDirectories = processedMergedBamFileService.singleLaneQAResultsDirectories(mergedBamFile)
        String temporalDestinationDir = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        Map<String, String> clusterPrefix = configService.clusterSpecificCommandPrefixes(project)
        String tmpQADestinationDirectory = processedMergedBamFileService.qaResultTempDestinationDirectory(mergedBamFile)
        String qaDestinationDirectory = processedMergedBamFileService.qaResultDestinationDirectory(mergedBamFile)
        Realm realm = configService.getRealmDataProcessing(project)
        String jobId = executionHelperService.sendScript(realm) {
            String text = ""
            for (String directoryName : singleLaneQAResultsDirectories.keySet()) {
                String src = singleLaneQAResultsDirectories.get(directoryName)
                text += """
# Remove old QA results directory for single lane BAM files if they exist
${clusterPrefix.exec} \"rm -r -f ${qaDestinationDirectory}/${directoryName}\"
${clusterPrefix.exec} \"mkdir -p -m 2750 ${tmpQADestinationDirectory}/${directoryName}\"
${clusterPrefix.cp} -r ${src}/* ${clusterPrefix.dest}${tmpQADestinationDirectory}/${directoryName}
${clusterPrefix.exec} \"find ${tmpQADestinationDirectory}/${directoryName} -type f -exec chmod 0640 '{}' \\;\"
"""
            }
            return text
        }
        log.debug "Job ${jobId} submitted to PBS"
    }
}
