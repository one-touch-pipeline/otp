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
        Map<String, String> clusterPrefix = configService.clusterSpecificCommandPrefixes(project)
        String tmpQADestinationDirectory = processedMergedBamFileService.qaResultTempDestinationDirectory(mergedBamFile)
        String qaDestinationDirectory = processedMergedBamFileService.qaResultDestinationDirectory(mergedBamFile)
        Realm realm = configService.getRealmDataProcessing(project)
        executionHelperService.sendScript(realm) {
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
            if (!text) {
                assert singleLaneQAResultsDirectories.empty
                // This happens if only merged BAM files but no single lane BAM files have been merged.
                // Submit a script text containing just a space, otherwise ExecutionService would complain.
                // (Just exiting here is not an option, because the mandatory output parameters __pbsRealm and __pbsIds have to be set.)
                text = " "
            }
            return text
        }
    }
}
