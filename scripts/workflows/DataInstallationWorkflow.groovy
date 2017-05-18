import de.dkfz.tbi.otp.job.jobs.dataInstallation.*
import de.dkfz.tbi.otp.job.processing.*

import static de.dkfz.tbi.otp.job.processing.PbsOptionMergingService.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = 'DataInstallationWorkflow'

plan(workflow) {
    start("start", "dataInstallationStartJob")
    job("copyFilesToFinalLocation", "copyFilesJob")
    job("createViewByPid", "createViewByPidJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}


println ctx.processingOptionService.createOrUpdate(
  "${PBS_PREFIX}${CopyFilesJob.simpleName}",
  null,
  null,
  '{"-l": { walltime: "12:00:00"}}',
  "set the walltime for the CopyFilesJob to 2h to get in the faster queue"
)

ctx.processingOptionService.createOrUpdate(
        AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME,
        workflow,
        null,
        '50',
        ''
)

ctx.processingOptionService.createOrUpdate(
        AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME,
        workflow,
        null,
        '25',
        ''
)
