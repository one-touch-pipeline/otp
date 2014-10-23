import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

final String WORKFLOW_NAME = 'SamplePairDiscoveryWorkflow'

plan(WORKFLOW_NAME, ctx, true) {
    start('samplePairDiscoveryStartJob', 'samplePairDiscoveryStartJob')
    job('samplePairDiscoveryJob', 'samplePairDiscoveryJob')
}

println ctx.processingOptionService.createOrUpdate(TOTAL_SLOTS_OPTION_NAME, WORKFLOW_NAME, null, '1', '')
println ctx.processingOptionService.createOrUpdate(SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, WORKFLOW_NAME, null, '0', '')
