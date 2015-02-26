import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("SnvWorkflow") {
    start("start", "snvStartJob")
    job("snvCalling", "snvCallingJob") {
        outputParameter("${REALM}")
        outputParameter("${SCRIPT}")
    }
    job("copySnvCallingResults", "clusterScriptExecutorJob") {
        inputParameter("${REALM}", "snvCalling", "${REALM}")
        inputParameter("${SCRIPT}", "snvCalling", "${SCRIPT}")
    }
    job("snvAnnotation", "snvAnnotationJob")
    job("snvDeepAnnotation", "snvDeepAnnotationJob") {
        outputParameter("${REALM}")
        outputParameter("${SCRIPT}")
    }
    job("copySnvDeepAnnotationResults", "clusterScriptExecutorJob") {
        inputParameter("${REALM}", "snvDeepAnnotation", "${REALM}")
        inputParameter("${SCRIPT}", "snvDeepAnnotation", "${SCRIPT}")
    }
    job("filterVcf", "filterVcfJob") {
        outputParameter("${REALM}")
        outputParameter("${SCRIPT}")
    }
    job("copyFilterVcfResults", "clusterScriptExecutorJob") {
        inputParameter("${REALM}", "filterVcf", "${REALM}")
        inputParameter("${SCRIPT}", "filterVcf", "${SCRIPT}")
    }
    job("snvCompletion", "snvCompletionJob")
}


println ctx.processingOptionService.createOrUpdate(
    "PBS_snvPipeline_CALLING_WGS",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "24:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WGS calling job"
)

println ctx.processingOptionService.createOrUpdate(
    "PBS_snvPipeline_CALLING_WES",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "08:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WES calling job"
)



println ctx.processingOptionService.createOrUpdate(
    "PBS_snvPipeline_SNV_ANNOTATION_WGS",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "48:00:00", mem: "3g"}}',
    "suggestion of the CO group (Ivo) for the snv WGS annotation job"
)

println ctx.processingOptionService.createOrUpdate(
    "PBS_snvPipeline_SNV_ANNOTATION_WES",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "24:00:00", mem: "3g"}}',
    "suggestion of the CO group (Ivo) for the snv WES annotation job"
)



println ctx.processingOptionService.createOrUpdate(
    "PBS_snvPipeline_SNV_DEEPANNOTATION_WGS",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=3:lsdf", walltime: "04:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WGS deep annotation job"
)

println ctx.processingOptionService.createOrUpdate(
    "PBS_snvPipeline_SNV_DEEPANNOTATION_WES",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=3:lsdf", walltime: "02:00:00", mem: "400m"}}',
    "suggestion of the CO group (Ivo) for the snv WES deep annotation job"
)



println ctx.processingOptionService.createOrUpdate(
    "PBS_snvPipeline_FILTER_VCF_WGS",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "04:00:00", mem: "1g"}}',
    "suggestion of the CO group (Ivo) for the snv WGS filter job"
)

println ctx.processingOptionService.createOrUpdate(
    "PBS_snvPipeline_FILTER_VCF_WES",
    "DKFZ",
    null,
    '{"-l": {nodes: "1:ppn=1:lsdf", walltime: "02:00:00", mem: "1g"}}',
    "suggestion of the CO group (Ivo) for the snv WES filter job"
)