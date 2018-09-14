import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "RnaAlignmentWorkflow"

plan(workflowName, ctx, true) {
    start("rnaAlignmentStart", "rnaAlignmentStartJob")
    job("executeRnaAlignment", "executeRnaAlignmentJob")
    job("parseRnaAlignmentQc", "parseRnaAlignmentQcJob")
    job("linkRnaAlignmentFilesToFinalDestination", "linkRnaAlignmentFilesToFinalDestinationJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(MAXIMUM_NUMBER_OF_JOBS, '5', workflowName)
processingOptionService.createOrUpdate(MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, '0', workflowName)
