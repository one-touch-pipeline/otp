package de.dkfz.tbi.otp.job.jobs.merging

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.FileNotReadableException

class MergingValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    MergingPassService mergingPassService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)

        boolean state = processedMergedBamFileService.updateBamFile(mergedBamFile)
        state ? succeed() : fail()

        //TODO check of metricsPath ? if yes: here or in a separate validation job? if here, how to combine?
    }
}
