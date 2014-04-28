package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class TransferMergedBamFileJob extends AbstractJobImpl {

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
        Map<String, String> clusterPrefix = configService.clusterSpecificCommandPrefixes(project)
        Realm realm = configService.getRealmDataProcessing(project)
        executionHelperService.sendScript(realm) { """
cd ${locations.sourceDirectory}
${clusterPrefix.cp} *.bam *.bai *.md5sum ${clusterPrefix.dest}${temporalDestinationDir}
${clusterPrefix.exec} \"find ${temporalDestinationDir} -type f -exec chmod 0640 '{}' \\;\"
""" }
    }
}
