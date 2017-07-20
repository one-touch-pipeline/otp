import com.google.common.base.CaseFormat
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
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

println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvCallingJob.simpleName}",
        null,
        '{"-l": {nodes: "1:ppn=1", walltime: "24:00:00", mem: "400m"}}'
)

println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvCallingJob.simpleName}_${exome}",
        null,
        '{"-l": {walltime: "08:00:00"}}'
)



println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvJoiningJob.simpleName}",
        null,
        '{"-l": {nodes: "1:ppn=1", walltime: "24:00:00", mem: "400m"}}'
)

println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvJoiningJob.simpleName}_${exome}",
        null,
        '{"-l": {walltime: "08:00:00"}}'
)



println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvAnnotationJob.simpleName}",
        null,
        '{"-l": {nodes: "1:ppn=1", walltime: "96:00:00", mem: "3g"}}'
)

println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvAnnotationJob.simpleName}_${exome}",
        null,
        '{"-l": {walltime: "24:00:00"}}'
)



println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvDeepAnnotationJob.simpleName}",
        null,
        '{"-l": {nodes: "1:ppn=4", walltime: "04:00:00", mem: "400m"}}'
)

println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${SnvDeepAnnotationJob.simpleName}_${exome}",
        null,
        '{"-l": {nodes: "1:ppn=3", walltime: "02:00:00"}}'
)



println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${FilterVcfJob.simpleName}",
        null,
        '{"-l": {nodes: "1:ppn=1", walltime: "04:00:00", mem: "3g"}}'
)

println ctx.processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${FilterVcfJob.simpleName}_${exome}",
        null,
        '{"-l": {walltime: "02:00:00", mem: "1g"}}'
)


// number of all SNV Calling workflows which can be executed in parallel
println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, workflowName, null, '100')
/*
 number of slots which are reserved only for FastTrack Workflows
 -> OptionName.MAXIMUM_NUMBER_OF_JOBS - OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK slots can be used by workflow runs with a normal priority
  */
println ctx.processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflowName, null, '84')
