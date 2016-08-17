package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Component
@Scope("prototype")
@UseJobLog
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
