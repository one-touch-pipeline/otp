import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = "RoddySnvWorkflow"

plan(workflow, ctx, true) {
    start("roddySnvStart", "roddySnvStartJob")
    job("executeRoddySnv", "executeRoddySnvJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, workflow, null, '5')
println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflow, null, '0')
