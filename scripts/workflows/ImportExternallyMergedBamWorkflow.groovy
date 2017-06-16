import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = 'ImportExternallyMergedBamWorkflow'

plan(workflow) {

    start("start", "importExternallyMergedBamStartJob")
    job("importExternallyMergedBam", "importExternallyMergedBamJob")
    job("replaceSourceWithLink", "replaceSourceWithLinkJob")
}

ctx.processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS,
        workflow,
        null,
        '2'
)

ctx.processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK,
        workflow,
        null,
        '0'
)