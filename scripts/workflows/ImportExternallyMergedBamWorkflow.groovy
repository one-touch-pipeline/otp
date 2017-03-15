import de.dkfz.tbi.otp.job.processing.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*


String workflow = 'ImportExternallyMergedBamWorkflow'

plan(workflow) {

    start("start", "importExternallyMergedBamStartJob")
    job("importExternallyMergedBam", "importExternallyMergedBamJob")
    job("replaceSourceWithLink", "replaceSourceWithLinkJob")
}

ctx.processingOptionService.createOrUpdate(
        AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME,
        workflow,
        null,
        '2',
        ''
)

ctx.processingOptionService.createOrUpdate(
        AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME,
        workflow,
        null,
        '0',
        ''
)