import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = "WgbsAlignmentWorkflow"

plan(workflow, ctx, true) {
    start("WgbsAlignmentStart", "WgbsAlignmentStartJob")
    job("executeWgbsAlignment", "executeWgbsAlignmentJob")
    job("parseWgbsAlignmentQc", "parseWgbsAlignmentQcJob")
    job("linkWgbsAlignmentFiles", "linkWgbsAlignmentFilesJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, workflow, null, '5')
println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflow, null, '0')
