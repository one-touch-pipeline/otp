import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "SnvWorkflow"

plan(workflowName) {
    start("start", "snvStartJob")
    job("snvCalling", "snvCallingJob")
    job("snvJoining", "snvJoiningJob")
    job("snvAnnotation", "snvAnnotationJob")
    job("snvDeepAnnotation", "snvDeepAnnotationJob")
    job("filterVcf", "filterVcfJob")
    job("snvCompletion", "snvCompletionJob")
    job("notifyProcessFinished", "notifyProcessFinishedJob")
}

String exome = SeqType.exomePairedSeqType.processingOptionName

ProcessingOptionService processingOptionService = ctx.processingOptionService

println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvCallingJob.simpleName}",
        null,
        '{"WALLTIME":"PT24H","MEMORY":"400m","CORES":"1"}',
)

println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvCallingJob.simpleName}_${exome}",
        null,
        '{"WALLTIME":"PT8H"}',
)



println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvJoiningJob.simpleName}",
        null,
        '{"WALLTIME":"PT24H","MEMORY":"400m","CORES":"1"}',
)

println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvJoiningJob.simpleName}_${exome}",
        null,
        '{"WALLTIME":"PT8H"}',
)



println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvAnnotationJob.simpleName}",
        null,
        '{"WALLTIME":"PT96H","MEMORY":"3g","CORES":"1"}',
)

println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvAnnotationJob.simpleName}_${exome}",
        null,
        '{"WALLTIME":"PT24H"}',
)



println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvDeepAnnotationJob.simpleName}",
        null,
        '{"WALLTIME":"PT4H","MEMORY":"400m","CORES":"4"}',
)

println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvDeepAnnotationJob.simpleName}_${exome}",
        null,
        '{"WALLTIME":"PT2H","CORES":"3"}',
)



println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${FilterVcfJob.simpleName}",
        null,
        '{"WALLTIME":"PT4H","MEMORY":"3g","CORES":"1"}',
)

println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${FilterVcfJob.simpleName}_${exome}",
        null,
        '{"WALLTIME":"PT2H","MEMORY":"1g"}',
)


// number of all SNV Calling workflows which can be executed in parallel
println processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, workflowName, null, '100')
/*
 number of slots which are reserved only for FastTrack Workflows
 -> OptionName.MAXIMUM_NUMBER_OF_JOBS - OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK slots can be used by workflow runs with a normal priority
  */
println processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflowName, null, '84')
