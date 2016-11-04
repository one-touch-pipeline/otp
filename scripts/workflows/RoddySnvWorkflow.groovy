import de.dkfz.tbi.otp.tracking.OtrsTicket

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl;

String workflow = "RoddySnvWorkflow"

plan(workflow, ctx, true) {
    start("roddySnvStart", "roddySnvStartJob")
    job("executeRoddySnv", "executeRoddySnvJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob") {
        constantParameter("step", OtrsTicket.ProcessingStep.SNV.name())
    }
}

println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME, workflow, null, '5', '')
println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflow, null, '0', '')
