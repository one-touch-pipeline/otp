import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.dataprocessing.*

plan("SnvWorkflow") {
    start("start", "snvStartJob")
    job("snvCalling", "snvCallingJob") {
    job("snvAnnotation", "snvAnnotationJob")
    job("indelAnnotation", "indelAnnotationJob")
    job("snvDeepAnnotation", "snvDeepAnnotationJob")
    job("indelDeepAnnotation", "indelDeepAnnotationJob")
    job("filterVcf", "filterVcfJob")
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
