import de.dkfz.tbi.otp.job.processing.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "SophiaWorkflow"

plan(workflowName, ctx, true) {
    start("roddySophiaStart", "roddySophiaStartJob")
    job("executeRoddySophia", "executeRoddySophiaJob")
    job("parseSophiaQc", "parseSophiaQcJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME, workflowName, null, '5', '')
println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflowName, null, '0', '')
