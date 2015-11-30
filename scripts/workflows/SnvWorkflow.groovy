import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import static de.dkfz.tbi.otp.job.processing.PbsOptionMergingService.PBS_PREFIX

import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.SeqType

plan("SnvWorkflow") {
    start("start", "snvStartJob")
    job("snvCalling", "snvCallingJob")
    job("snvJoining", "snvJoiningJob")
    job("snvAnnotation", "snvAnnotationJob")
    job("snvDeepAnnotation", "snvDeepAnnotationJob")
    job("filterVcf", "filterVcfJob")
    job("snvCompletion", "snvCompletionJob")
}

String wgs = SeqType.wholeGenomePairedSeqType.processingOptionName
String exome = SeqType.exomePairedSeqType.processingOptionName



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvCallingJob.simpleName}_${wgs}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "24:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WGS calling job"
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvCallingJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "08:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WES calling job"
)



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvJoiningJob.simpleName}_${wgs}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1", walltime: "24:00:00", mem: "400m"}}',
    ""
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvJoiningJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1", walltime: "08:00:00", mem: "400m"}}',
    ""
)



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvAnnotationJob.simpleName}_${wgs}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "48:00:00", mem: "3g"}}',
    "suggestion of the CO group (Ivo) for the snv WGS annotation job"
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvAnnotationJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "24:00:00", mem: "3g"}}',
    "suggestion of the CO group (Ivo) for the snv WES annotation job"
)



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvDeepAnnotationJob.simpleName}_${wgs}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=3:lsdf", walltime: "04:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WGS deep annotation job"
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${SnvDeepAnnotationJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=3:lsdf", walltime: "02:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WES deep annotation job"
)



println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${FilterVcfJob.simpleName}_${wgs}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "04:00:00", mem: "1g"}}',
    "suggestion of the CO group (Ivo) for the snv WGS filter job"
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${FilterVcfJob.simpleName}_${exome}",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "02:00:00", mem: "1g"}}',
    "suggestion of the CO group (Ivo) for the snv WES filter job"
)
