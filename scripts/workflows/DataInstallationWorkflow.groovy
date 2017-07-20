import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.dataInstallation.*

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
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        CopyFilesJob.simpleName,
        null,
        '{"-l": { walltime: "12:00:00"}}'
)

ctx.processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS,
        workflow,
        null,
        '50'
)

ctx.processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK,
        workflow,
        null,
        '25'
)
