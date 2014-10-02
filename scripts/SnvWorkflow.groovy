import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.CopyJobParameter

plan("SnvWorkflow") {
    start("start", "snvStartJob")
    job("snvCalling", "snvCallingJob")
    job("snvAnnotation", "snvAnnotationJob") {
        outputParameter(CopyJobParameter.SOURCE_LOCATION)
        outputParameter(CopyJobParameter.TARGET_LOCATION)
        outputParameter(CopyJobParameter.LINK_LOCATION)
        outputParameter(CopyJobParameter.PROJECT)
    }
    job("copySnvAnnotationResults", "copyJob") {
        inputParameter(CopyJobParameter.SOURCE_LOCATION, "snvAnnotation", CopyJobParameter.SOURCE_LOCATION)
        inputParameter(CopyJobParameter.TARGET_LOCATION, "snvAnnotation", CopyJobParameter.TARGET_LOCATION)
        inputParameter(CopyJobParameter.LINK_LOCATION, "snvAnnotation", CopyJobParameter.LINK_LOCATION)
        inputParameter(CopyJobParameter.PROJECT, "snvAnnotation", CopyJobParameter.PROJECT)
    }
    job("indelAnnotation", "indelAnnotationJob") {
        outputParameter(CopyJobParameter.SOURCE_LOCATION)
        outputParameter(CopyJobParameter.TARGET_LOCATION)
        outputParameter(CopyJobParameter.LINK_LOCATION)
        outputParameter(CopyJobParameter.PROJECT)
    }
    job("copyIndelAnnotationResults", "copyJob") {
        inputParameter(CopyJobParameter.SOURCE_LOCATION, "indelAnnotation", CopyJobParameter.SOURCE_LOCATION)
        inputParameter(CopyJobParameter.TARGET_LOCATION, "indelAnnotation", CopyJobParameter.TARGET_LOCATION)
        inputParameter(CopyJobParameter.LINK_LOCATION, "indelAnnotation", CopyJobParameter.LINK_LOCATION)
        inputParameter(CopyJobParameter.PROJECT, "indelAnnotation", CopyJobParameter.PROJECT)
    }
    job("snvDeepAnnotation", "snvDeepAnnotationJob") {
        outputParameter(CopyJobParameter.SOURCE_LOCATION)
        outputParameter(CopyJobParameter.TARGET_LOCATION)
        outputParameter(CopyJobParameter.LINK_LOCATION)
        outputParameter(CopyJobParameter.PROJECT)
    }
    job("copySnvDeepAnnotationResults", "copyJob") {
        inputParameter(CopyJobParameter.SOURCE_LOCATION, "snvDeepAnnotation", CopyJobParameter.SOURCE_LOCATION)
        inputParameter(CopyJobParameter.TARGET_LOCATION, "snvDeepAnnotation", CopyJobParameter.TARGET_LOCATION)
        inputParameter(CopyJobParameter.LINK_LOCATION, "snvDeepAnnotation", CopyJobParameter.LINK_LOCATION)
        inputParameter(CopyJobParameter.PROJECT, "snvDeepAnnotation", CopyJobParameter.PROJECT)
    }
    job("indelDeepAnnotation", "indelDeepAnnotationJob") {
        outputParameter(CopyJobParameter.SOURCE_LOCATION)
        outputParameter(CopyJobParameter.TARGET_LOCATION)
        outputParameter(CopyJobParameter.LINK_LOCATION)
        outputParameter(CopyJobParameter.PROJECT)
    }
    job("copyIndelDeepAnnotationResults", "copyJob") {
        inputParameter(CopyJobParameter.SOURCE_LOCATION, "indelDeepAnnotation", CopyJobParameter.SOURCE_LOCATION)
        inputParameter(CopyJobParameter.TARGET_LOCATION, "indelDeepAnnotation", CopyJobParameter.TARGET_LOCATION)
        inputParameter(CopyJobParameter.LINK_LOCATION, "indelDeepAnnotation", CopyJobParameter.LINK_LOCATION)
        inputParameter(CopyJobParameter.PROJECT, "indelDeepAnnotation", CopyJobParameter.PROJECT)
    }
    job("filterVcf", "filterVcfJob") {
        outputParameter(CopyJobParameter.SOURCE_LOCATION)
        outputParameter(CopyJobParameter.TARGET_LOCATION)
        outputParameter(CopyJobParameter.LINK_LOCATION)
        outputParameter(CopyJobParameter.PROJECT)
    }
    job("copyFilterVcfResults", "copyJob") {
        inputParameter(CopyJobParameter.SOURCE_LOCATION, "filterVcf", CopyJobParameter.SOURCE_LOCATION)
        inputParameter(CopyJobParameter.TARGET_LOCATION, "filterVcf", CopyJobParameter.TARGET_LOCATION)
        inputParameter(CopyJobParameter.LINK_LOCATION, "filterVcf", CopyJobParameter.LINK_LOCATION)
        inputParameter(CopyJobParameter.PROJECT, "filterVcf", CopyJobParameter.PROJECT)
    }
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
