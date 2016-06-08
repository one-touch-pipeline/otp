import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.tracking.OtrsTicket

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = 'FastqcWorkflow'

plan(workflow) {
    /**
     * Loads an unprocessed SeqTrack object from database and stores it as a process parameter
     */
    start("start", "fastqcStartJob")
    job("fastqc", "fastqcJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob") {
        constantParameter("step", OtrsTicket.ProcessingStep.FASTQC.name())
    }
}


ctx.processingOptionService.createOrUpdate(
        "fastqcCommand",
        null,
        null,
        "fastqc-0.10.1 --java /path/to/programs/jdk/jdk1.6.0_45/bin/java",
        "command for fastqc with java"
)

ctx.processingOptionService.createOrUpdate(
        AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME,
        workflow,
        null,
        '100',
        ''
)

ctx.processingOptionService.createOrUpdate(
        AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME,
        workflow,
        null,
        '50',
        ''
)
