import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = 'FastqcWorkflow'

plan(workflow) {
    /**
     * Loads an unprocessed SeqTrack object from database and stores it as a process parameter
     */
    start("start", "fastqcStartJob")
    job("fastqc", "fastqcJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

processingOptionService.createOrUpdate(MAXIMUM_NUMBER_OF_JOBS, '5', workflow)
processingOptionService.createOrUpdate(MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, '0', workflow)
