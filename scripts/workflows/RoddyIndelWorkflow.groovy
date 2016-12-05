import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.tracking.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "IndelWorkflow"

plan(workflowName, ctx, true) {
    start("roddyIndelStart", "roddyIndelStartJob")
    job("executeRoddyIndel", "executeRoddyIndelJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob") {
        constantParameter("step", OtrsTicket.ProcessingStep.INDEL.name())
    }
}

println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME, workflowName, null, '5', '')
println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflowName, null, '0', '')
