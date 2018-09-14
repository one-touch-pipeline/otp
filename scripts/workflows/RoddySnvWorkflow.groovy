import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = "RoddySnvWorkflow"

plan(workflow, ctx, true) {
    start("roddySnvStart", "roddySnvStartJob")
    job("executeRoddySnv", "executeRoddySnvJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, '5', workflow)
processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, '0', workflow)
