package de.dkfz.tbi.otp.job.jobs.merging

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*

class MergingValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    MergingJob mergingJob

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile mergedBamFile = exactlyOneElement(ProcessedMergedBamFile.findAllByMergingPass(mergingPass))
        try {
            mergingJob.createInputFileString(mergedBamFile)
        } catch (AssertionError e) {
            throw new RuntimeException('An input BAM file seems to have changed on the file system while this job was processing it.', e)
        }
        boolean state = processedMergedBamFileService.updateBamFile(mergedBamFile)
        state &= processedMergedBamFileService.updateBamMetricsFile(mergedBamFile)
        state &= processedMergedBamFileService.updateBamFileIndex(mergedBamFile)
        state ? succeed() : fail()
    }
}
