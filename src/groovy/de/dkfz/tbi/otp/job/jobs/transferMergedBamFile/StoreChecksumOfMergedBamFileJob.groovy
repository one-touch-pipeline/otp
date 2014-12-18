package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.scheduler.ProcessStatusService
import de.dkfz.tbi.otp.ngsdata.ChecksumFileService

class StoreChecksumOfMergedBamFileJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ChecksumFileService checksumFileService

    @Override
    public void execute() throws Exception {
        long id = Long.parseLong(getProcessParameterValue())
        ProcessedMergedBamFile file = ProcessedMergedBamFile.get(id)
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(file)

        String md5SumFile = locations.get("destinationDirectory") + "/" + locations.get("md5BamFile")
        String md5Bam = checksumFileService.firstMD5ChecksumFromFile(md5SumFile)
        boolean successfulSave = processedMergedBamFileService.storeMD5Digest(file, md5Bam)
        //TODO: processedMergedBamFile somehow has to be stored as MergedAlignmentDataFile or another GUI representation of the files.
        //This can not be done now, since the db structure can not be used as it is now
        log.debug "Stored MD5 digest for merged BAM file " + locations.get("bamFile") + " (id= " + file + " ) in database"
        if (successfulSave) {
            succeed()
        } else {
            throw new RuntimeException("The md5sums in ${md5SumFile} are not equal to the md5sums in the final folder")
        }
    }
}
