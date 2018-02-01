import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.dataInstallation.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflow = 'DataInstallationWorkflow'

plan(workflow) {
    start("start", "dataInstallationStartJob")
    job("copyFilesToFinalLocation", "copyFilesJob")
    job("createViewByPid", "createViewByPidJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        CopyFilesJob.simpleName,
        null,
        '{"WALLTIME":"PT12H"}',
)

processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS,
        workflow,
        null,
        '5'
)

processingOptionService.createOrUpdate(
        OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK,
        workflow,
        null,
        '0'
)
