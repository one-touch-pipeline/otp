import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.getSLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME
import static de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl.getTOTAL_SLOTS_OPTION_NAME
import static de.dkfz.tbi.otp.job.processing.PbsOptionMergingService.PBS_PREFIX
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

import de.dkfz.tbi.otp.job.jobs.merging.*
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import de.dkfz.tbi.otp.ngsdata.SeqType

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

//special pbs options for merging workflow
println ctx.processingOptionService.createOrUpdate(
  "${PBS_PREFIX}${MergingJob.simpleName}",
  "DKFZ",
  null,
  '{"-l": {nodes: "1:ppn=6", walltime: "100:00:00", mem: "25g"}}',
  "merging job depending cluster option for dkfz"
)
println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${MergingJob.simpleName}_${SeqType.exomePairedSeqType.processingOptionName}",
    Cluster.DKFZ.toString(),
    null,
    '{"-l": {mem: "15g"}}',
    ''
)

//picard option for mark duplicates
println ctx.processingOptionService.createOrUpdate(
  "picardMdup",
  null,
  null,
  'VALIDATION_STRINGENCY=SILENT REMOVE_DUPLICATES=FALSE ASSUME_SORTED=TRUE MAX_RECORDS_IN_RAM=12500000 CREATE_INDEX=TRUE CREATE_MD5_FILE=TRUE',
  "picard option used in duplicates marking"
)



// picard program for merging job
ctx.processingOptionService.createOrUpdate(
        'picardMdupCommand',
        null,
        null,
        'picard-1.61.sh MarkDuplicates',
        'command for versioned picard'
)


// number of all merging workflows which can be executed in parallel
println ctx.processingOptionService.createOrUpdate(TOTAL_SLOTS_OPTION_NAME, workflowName, null, '60', '')
// number of slots which are reserved only for FastTrack Workflows
println ctx.processingOptionService.createOrUpdate(SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, workflowName, null, '30', '')

