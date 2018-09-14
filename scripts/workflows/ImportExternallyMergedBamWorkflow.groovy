import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = 'ImportExternallyMergedBamWorkflow'

plan(workflow) {

    start("start", "importExternallyMergedBamStartJob")
    job("importExternallyMergedBam", "importExternallyMergedBamJob")
    job("replaceSourceWithLink", "replaceSourceWithLinkJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS,
        '2',
        workflow,
)

processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK,
        '0',
        workflow,
)
