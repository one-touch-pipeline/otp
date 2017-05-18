import de.dkfz.tbi.otp.job.processing.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = "WgbsAlignmentWorkflow"

plan(workflow, ctx, true) {
    start("WgbsAlignmentStart", "WgbsAlignmentStartJob")
    job("executeWgbsAlignment", "executeWgbsAlignmentJob")
    job("parseWgbsAlignmentQc", "parseWgbsAlignmentQcJob")
    job("linkWgbsAlignmentFiles", "linkWgbsAlignmentFilesJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME, workflow, null, '5', '')
println ctx.processingOptionService.createOrUpdate(AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflow, null, '0', '')
