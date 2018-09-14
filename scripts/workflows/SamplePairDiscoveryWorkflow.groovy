import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

final String WORKFLOW_NAME = 'SamplePairDiscoveryWorkflow'

plan(WORKFLOW_NAME, ctx, true) {
    start('samplePairDiscoveryStartJob', 'samplePairDiscoveryStartJob')
    job('samplePairDiscoveryJob', 'samplePairDiscoveryJob')
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, '1', WORKFLOW_NAME)
processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, '0', WORKFLOW_NAME)
