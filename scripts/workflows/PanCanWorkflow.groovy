import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = "PanCanWorkflow"

plan(workflow, ctx, true) {
    start("PanCanStart", "PanCanStartJob")
    job("executePanCan", "executePanCanJob")
    job("parsePanCanQc", "parsePanCanQcJob")
    job("movePanCanFilesToFinalDestination", "movePanCanFilesToFinalDestinationJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(MAXIMUM_NUMBER_OF_JOBS, workflow, null, '5')
processingOptionService.createOrUpdate(MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflow, null, '0')
