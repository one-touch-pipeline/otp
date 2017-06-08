import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.merging.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "MergingWorkflow"

plan(workflowName) {
    start("start", "mergingStartJob")
    job("createOutputDirectory", "mergingCreateOutputDirectoryJob")
    job("merging", "mergingJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("mergingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "merging", "__pbsIds")
        inputParameter("__pbsRealm", "merging", "__pbsRealm")
    }
    job("mergingValidation", "mergingValidationJob")
    job("metricsParsing", "metricsParsingJob")
    job("mergingComplete", "mergingCompleteJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

//special pbs options for merging workflow
println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${MergingJob.simpleName}",
        null,
        '{"WALLTIME":"PT100H","MEMORY":"25g","CORES":"6"}',
)
println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${MergingJob.simpleName}_${SeqType.exomePairedSeqType.processingOptionName}",
        null,
        '{"MEMORY":"15g"}',
)

//picard option for mark duplicates
println processingOptionService.createOrUpdate(
        OptionName.PIPELINE_OTP_ALIGNMENT_PICARD_MDUP,
        null,
        null,
        'VALIDATION_STRINGENCY=SILENT REMOVE_DUPLICATES=FALSE ASSUME_SORTED=TRUE MAX_RECORDS_IN_RAM=12500000 CREATE_INDEX=TRUE CREATE_MD5_FILE=TRUE'
)



// picard program for merging job
processingOptionService.createOrUpdate(
        OptionName.COMMAND_PICARD_MDUP,
        null,
        null,
        'picard-1.61.sh MarkDuplicates'
)


// number of all merging workflows which can be executed in parallel
println processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, workflowName, null, '60')
// number of slots which are reserved only for FastTrack Workflows
println processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflowName, null, '30')

