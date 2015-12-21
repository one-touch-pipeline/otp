import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME
import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.TOTAL_SLOTS_OPTION_NAME
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import static de.dkfz.tbi.otp.job.processing.PbsOptionMergingService.PBS_PREFIX

import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.SeqType

String workflowName = "SnvWorkflow"

plan(workflowName) {
    start("start", "snvStartJob")
    job("snvCalling", "snvCallingJob")
    job("snvJoining", "snvJoiningJob")
    job("snvAnnotation", "snvAnnotationJob")
    job("snvDeepAnnotation", "snvDeepAnnotationJob")
    job("filterVcf", "filterVcfJob")
    job("snvCompletion", "snvCompletionJob")
}

String exome = SeqType.exomePairedSeqType.processingOptionName



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvCallingJob.simpleName}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1", walltime: "24:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WGS calling job"
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvCallingJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {walltime: "08:00:00"}}',
    "suggestion of the CO group (Ivo) for the snv WES calling job"
)



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvJoiningJob.simpleName}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1", walltime: "24:00:00", mem: "400m"}}',
    ""
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvJoiningJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {walltime: "08:00:00"}}',
    ""
)



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvAnnotationJob.simpleName}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1", walltime: "48:00:00", mem: "3g"}}',
    "suggestion of the CO group (Ivo) for the snv WGS annotation job"
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvAnnotationJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {walltime: "24:00:00"}}',
    "suggestion of the CO group (Ivo) for the snv WES annotation job"
)



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvDeepAnnotationJob.simpleName}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=4", walltime: "04:00:00", mem: "400m"}}',
    ""
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvDeepAnnotationJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=3", walltime: "02:00:00"}}',
    "suggestion of the CO group (Ivo) for the snv WES deep annotation job"
)



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${FilterVcfJob.simpleName}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1", walltime: "04:00:00", mem: "3g"}}',
    ""
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${FilterVcfJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {walltime: "02:00:00", mem: "1g"}}',
    "suggestion of the CO group (Ivo) for the snv WES filter job"
)


// number of all SNV Calling workflows which can be executed in parallel
println ctx.processingOptionService.createOrUpdate(TOTAL_SLOTS_OPTION_NAME, workflowName, null, '100', '')
/*
 number of slots which are reserved only for FastTrack Workflows
 -> TOTAL_SLOTS_OPTION_NAME - SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME slots can be used by workflow runs with a normal priority
  */
println ctx.processingOptionService.createOrUpdate(SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflowName, null, '84', '')