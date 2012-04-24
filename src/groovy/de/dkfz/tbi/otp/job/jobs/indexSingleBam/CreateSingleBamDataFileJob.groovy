package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.*

class CreateSingleBamDataFileJob extends AbstractEndStateAwareJobImpl {

    SeqScan scan
    MergingLog mergingLog

    @Autowired
    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Override
    public void execute() throws Exception {
        long scanId = Long.parseLong(getProcessParameterValue())
        scan = SeqScan.get(scanId)
        mergingLog = MergingLog.findBySeqScan(scan)
        String fileSystem = mergedAlignmentDataFileService.pathToHost(scan)
        String filePath = mergedAlignmentDataFileService.buildRelativePath(mergingLog)
        String fileName = mergedAlignmentDataFileService.buildFileName(mergingLog)
        MergedAlignmentDataFile dataFile = new MergedAlignmentDataFile (
            fileSystem: fileSystem,
            fileName: fileName,
            filePath: filePath,
            mergingLog: mergingLog
        )
        if (dataFile.validate()) {
            dataFile.save(flush: true)
            succeed()
        } else {
            fail()
        }
    }

}
