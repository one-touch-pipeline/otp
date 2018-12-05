import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = "CellRangerWorkflow"

plan(workflow, ctx, true) {
    start("start", "cellRangerAlignmentStartJob")
    job("executeCellRanger", "executeCellRangerJob")
    job("parseCellRangerQc", "parseCellRangerQcJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(MAXIMUM_NUMBER_OF_JOBS, '5', workflow)
processingOptionService.createOrUpdate(MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, '0', workflow)
