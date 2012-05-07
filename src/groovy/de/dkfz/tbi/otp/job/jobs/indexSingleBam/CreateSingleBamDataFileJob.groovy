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
        MergedAlignmentDataFile dataFile = new MergedAlignmentDataFile (
            fileSystem: mergedAlignmentDataFileService.pathToHost(scan),
            fileName: mergedAlignmentDataFileService.buildFileName(mergingLog),
            filePath: mergedAlignmentDataFileService.buildRelativePath(mergingLog),
            mergingLog: MergingLog.findBySeqScan(scan)
        )
        if (dataFile.validate()) {
            dataFile.save(flush: true)
            succeed()
        } else {
            fail()
        }
    }

}
