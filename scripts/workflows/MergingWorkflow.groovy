import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.dataprocessing.*

plan("MergingWorkflow") {
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

//special pbs options for merging workflow
println ctx.processingOptionService.createOrUpdate(
  "PBS_mergingJob",
  "DKFZ",
  null,
  '{"-l": {nodes: "1:ppn=6:lsdf", walltime: "100:00:00", mem: "50g"}}',
  "merging job depending cluster option for dkfz"
)

//picard option for mark duplicates
println ctx.processingOptionService.createOrUpdate(
  "picardMdup",
  null,
  null,
  'VALIDATION_STRINGENCY=SILENT REMOVE_DUPLICATES=FALSE ASSUME_SORTED=TRUE MAX_RECORDS_IN_RAM=12500000 CREATE_INDEX=TRUE CREATE_MD5_FILE=TRUE',
  "picard option used in duplicates marking"
)
