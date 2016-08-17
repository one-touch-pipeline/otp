package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class MergingCompleteJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    MergingPassService mergingPassService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Override
    public void execute() throws Exception {
        long mergingPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        // Set state for the next steps
        mergingPassService.mergingPassFinishedAndStartQA(mergingPass)
        succeed()
    }
}
