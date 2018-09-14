import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "ACEseqWorkflow"

plan(workflowName, ctx, true) {
    start("roddyAceseqStart", "aceseqStartJob")
    job("executeRoddyAceseq", "executeRoddyAceseqJob")
    job("parseAceseqQc", "parseAceseqQcJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, '5', workflowName)
processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, '0', workflowName)
