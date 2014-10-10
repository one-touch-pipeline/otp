package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class MergingCompleteJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    MergingPassService mergingPassService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile processedMergedBamFile = exactlyOneElement(ProcessedMergedBamFile.findAllByMergingPass(mergingPass))
        // Update the property "numberOfMergedLanes" in the BAM file
        processedMergedBamFileService.updateNumberOfMergedLanes(processedMergedBamFile)
        // Set state for the next steps
        mergingPassService.mergingPassFinishedAndStartQA(mergingPass)
        succeed()
    }
}
