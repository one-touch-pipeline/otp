package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.*

//This Job is not used anymore in the workflow "IndexSingleBamWorkflow".
//It will be kept because of historical reasons and perhaps can be reused later.
@Deprecated
class CheckMergedBamFileJob extends AbstractEndStateAwareJobImpl {

    SeqScan scan

    @Autowired
    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        scan = SeqScan.get(scanId)
        MergedAlignmentDataFile dataFile = getDataFile()
        if (checkDataFile(dataFile)) {
            succeed()
        } else {
            fail()
        }
    }

    private boolean checkDataFile(MergedAlignmentDataFile dataFile) {
        String fullPath = mergedAlignmentDataFileService.getFullPath(dataFile)
        File file = new File(fullPath)
        if (!file.canRead()) {
            return false
        }
        dataFile.fileExists = true
        dataFile.fileSize = file.length()
        dataFile.fileSystemDate = lsdfFilesService.fileCreationDate(fullPath)
        dataFile.save(flush: true)
        return true
    }

    private MergedAlignmentDataFile getDataFile() {
        MergingLog mergingLog = MergingLog.findBySeqScan(scan)
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(mergingLog)
        return dataFile
    }
}