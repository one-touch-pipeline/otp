import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

final String WORKFLOW_NAME = 'SamplePairDiscoveryWorkflow'

plan(WORKFLOW_NAME, ctx, true) {
    start('samplePairDiscoveryStartJob', 'samplePairDiscoveryStartJob')
    job('samplePairDiscoveryJob', 'samplePairDiscoveryJob')
}

println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, WORKFLOW_NAME, null, '1')
println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, WORKFLOW_NAME, null, '0')
