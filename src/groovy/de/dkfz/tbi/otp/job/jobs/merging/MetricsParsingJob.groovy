package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

/**
 * Job to call {@link PicardMarkDuplicatesMetricsService#loadMetricsForProcessedMergedBamFile(ProcessedMergedBamFile)}
 *
 */
class MetricsParsingJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    PicardMarkDuplicatesMetricsService picardMarkDuplicatesMetricsService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)
        boolean state = picardMarkDuplicatesMetricsService.parseAndLoadMetricsForProcessedMergedBamFiles(processedMergedBamFile)
        state ? succeed() : fail()
    }
}
