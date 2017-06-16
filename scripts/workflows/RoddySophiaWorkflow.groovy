import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "SophiaWorkflow"

plan(workflowName, ctx, true) {
    start("roddySophiaStart", "roddySophiaStartJob")
    job("executeRoddySophia", "executeRoddySophiaJob")
    job("parseSophiaQc", "parseSophiaQcJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, workflowName, null, '5')
println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflowName, null, '0')
