package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class MergingFileIndexValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    MergingPassService mergingPassService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)

        boolean status = processedMergedBamFileService.updateBamFileIndex(mergedBamFile)
        if (status) {
            succeed()
        } else {
            fail()
        }
    }
}
