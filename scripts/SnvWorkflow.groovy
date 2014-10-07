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
  "PBS_snvPipeline_WGS",
  "DKFZ",
  null,
  '{"-l": {walltime: "20:00:00"}}',
  "according to the CO group (Ivo) 20h is enough for the snv WGS jobs"
)


println ctx.processingOptionService.createOrUpdate(
  "PBS_snvPipeline_WES",
  "DKFZ",
  null,
  '{"-l": {walltime: "5:00:00"}}',
  "according to the CO group (Ivo) 5h is enough for the snv WES jobs"
)
