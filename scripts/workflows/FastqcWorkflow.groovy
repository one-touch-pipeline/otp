import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

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


ctx.processingOptionService.createOrUpdate(
        OptionName.COMMAND_FASTQC,
        null,
        null,
        "fastqc-0.10.1 --java /path/to/programs/jdk/jdk1.6.0_45/bin/java"
)

ctx.processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS,
        workflow,
        null,
        '100'
)

ctx.processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK,
        workflow,
        null,
        '50'
)
