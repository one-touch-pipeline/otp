package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.*

class CheckIndexFileJob  extends AbstractEndStateAwareJobImpl {

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
        if (checkIndexDataFile(dataFile)) {
            succeed()
        } else {
            fail()
        }
    }

    private boolean checkIndexDataFile(MergedAlignmentDataFile dataFile) {
        String fullPath = mergedAlignmentDataFileService.getFullPath(dataFile)
        File file = new File("${fullPath}.bai")
        if (!file.canRead()) {
            return false
        }
        dataFile.indexFileExists = true
        dataFile.save(flush: true)
        return true
    }

    private MergedAlignmentDataFile getDataFile() {
        MergingLog mergingLog = MergingLog.findBySeqScan(scan)
        MergedAlignmentDataFile dataFile = MergedAlignmentDataFile.findByMergingLog(mergingLog)
        return dataFile
    }

}
