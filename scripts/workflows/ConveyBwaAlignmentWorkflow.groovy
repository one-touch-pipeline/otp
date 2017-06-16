import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.alignment.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.job.processing.PbsOptionMergingService.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

final String WORKFLOW_NAME = 'ConveyBwaAlignmentWorkflow'

plan(WORKFLOW_NAME, ctx, true) {
    start("start", "BwaAlignmentStartJob")
    job("createOutputDirectories", "createAlignmentOutputDirectoryJob")
    job("checkQualityEncoding", "checkQualityEncodingJob")
    job("conveyBwaAlignment", "conveyBwaAlignmentJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("conveyBwaAlignmentWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "conveyBwaAlignment", "__pbsIds")
        inputParameter("__pbsRealm", "conveyBwaAlignment", "__pbsRealm")
    }
    job("bwaAlignmentValidation", "bwaAlignmentValidationJob")
    job("bwaPairingAndSorting", "bwaPairingAndSortingJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("bwaPairingAndSortingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "bwaPairingAndSorting", "__pbsIds")
        inputParameter("__pbsRealm", "bwaPairingAndSorting", "__pbsRealm")
    }
    job("bwaPairingValidation", "bamFileValidationJob") {
        constantParameter("BamType", "SORTED")
    }
    job("sortedBamIndexing", "bamFileIndexingJob") {
        constantParameter("BamType", "SORTED")
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("sortedBamIndexingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "sortedBamIndexing", "__pbsIds")
        inputParameter("__pbsRealm", "sortedBamIndexing", "__pbsRealm")
    }
    job("sortedBamIndexingValidation", "bamFileIndexValidationJob") {
        constantParameter("BamType", "SORTED")
    }
    job("bwaAlignmentComplete", "bwaAlignmentCompleteJob")
}

// create Convey bwa number of cores option for conveyBwaAlignmentJob
ctx.processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_BWA_NUMBER_OF_CORES,
    null,
    null,
    '-t 12'
)

// create Convey bwa program option for conveyBwaAlignmentJob
ctx.processingOptionService.createOrUpdate(
    OptionName.COMMAND_CONVEY_BWA,
    null,
    null,
    'cnybwa-0.6.2'
)

// create Convey bwa program quality encoding option for PHRED
ctx.processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_QUALITY_ENCODING,
    'PHRED',
    null,
    ''
)

// create Convey bwa program quality encoding option for ILLUMINA
ctx.processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_QUALITY_ENCODING,
    'ILLUMINA',
    null,
    '-I'
)

// update bwa program option for bwaAlignmentJob
ctx.processingOptionService.createOrUpdate(
    OptionName.COMMAND_CONVEY_BWA,
    null,
    null,
    'bwa-0.6.2-tpx'
)

ctx.processingOptionService.createOrUpdate(
    OptionName.COMMAND_SAMTOOLS,
    null,
    null,
    'samtools-0.1.19'
)

// Create PBS options for bwaPairingAndSortingJob
ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${BwaPairingAndSortingJob.simpleName}",
    'DKFZ',
    null,
    '''{
      "-l": {
        "walltime": "50:00:00",
        "nodes": "1:ppn=6",
        "mem": "45g"
      }
    }'''
)

// Create PBS options for bwaAlignmentJob
ctx.processingOptionService.createOrUpdate(
        "${PBS_PREFIX}${ConveyBwaAlignmentJob.simpleName}",
        'DKFZ',
        null,
        '{"-l": {"walltime": "48:00:00", "nodes": "1:ppn=12", "mem": "126g"}, "-q": "convey", "-m": "a", "-S": "/bin/bash"}'
)

ctx.processingOptionService.createOrUpdate(
        "${PBS_PREFIX}${ConveyBwaAlignmentJob.simpleName}_${SeqType.exomePairedSeqType.processingOptionName}",
        'DKFZ',
        null,
        '{"-l": { "walltime": "2:00:00"}}'
)

ctx.processingOptionService.createOrUpdate(
    'numberOfSampeThreads',
    null,
    null,
    '-t 8'
)

ctx.processingOptionService.createOrUpdate(
    'numberOfSamToolsSortThreads',
    null,
    null,
    '-@ 8'
)

ctx.processingOptionService.createOrUpdate(
    'mbufferPairingSorting',
    null,
    null,
    '-m 2G'
)

ctx.processingOptionService.createOrUpdate(
    'samtoolsSortBuffer',
    null,
    null,
    '-m 2000000000'
)

ctx.processingOptionService.createOrUpdate(
    'bwaQParameter',
    null,
    null,
    '-q 20'
)

ctx.processingOptionService.createOrUpdate(
    ProcessingOption.Names.maximumNumberOfJobs,
    WORKFLOW_NAME,
    null,
    '100'
)
ctx.processingOptionService.createOrUpdate(
    Names.maximumNumberOfJobsReservedForFastTrack,
    WORKFLOW_NAME,
    null,
    '50'
)
