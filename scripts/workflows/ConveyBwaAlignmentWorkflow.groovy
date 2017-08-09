import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.jobs.alignment.*
import de.dkfz.tbi.otp.ngsdata.*

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

ProcessingOptionService processingOptionService = ctx.processingOptionService

// create Convey bwa number of cores option for conveyBwaAlignmentJob
processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_NUMBER_OF_CORES,
    null,
    null,
    '-t 12'
)

// create Convey bwa program option for conveyBwaAlignmentJob
processingOptionService.createOrUpdate(
    OptionName.COMMAND_CONVEY_BWA,
    null,
    null,
    'cnybwa-0.6.2'
)

// create Convey bwa program quality encoding option for PHRED
processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_QUALITY_ENCODING,
    'PHRED',
    null,
    ''
)

// create Convey bwa program quality encoding option for ILLUMINA
processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_CONVEY_BWA_QUALITY_ENCODING,
    'ILLUMINA',
    null,
    '-I'
)

// update bwa program option for bwaAlignmentJob
processingOptionService.createOrUpdate(
    OptionName.COMMAND_BWA,
    null,
    null,
    'bwa-0.6.2-tpx'
)

processingOptionService.createOrUpdate(
    OptionName.COMMAND_SAMTOOLS,
    null,
    null,
    'samtools-0.1.19'
)

// Create PBS options for bwaPairingAndSortingJob
processingOptionService.createOrUpdate(
    OptionName.CLUSTER_SUBMISSIONS_OPTION,
    "${BwaPairingAndSortingJob.simpleName}",
    null,
    '{"WALLTIME":"PT50H","MEMORY":"45g","CORES":"6"}',
)

// Create PBS options for bwaAlignmentJob
processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${ConveyBwaAlignmentJob.simpleName}",
        null,
        '{"QUEUE":"convey","WALLTIME":"PT48H","MEMORY":"126g","CORES":"12"}',
)

processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${ConveyBwaAlignmentJob.simpleName}_${SeqType.exomePairedSeqType.processingOptionName}",
        null,
        '{"WALLTIME":"PT2H"}',
)

processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_NUMBER_OF_SAMPE_THREADS,
    null,
    null,
    '-t 8'
)

processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_NUMBER_OF_SAMTOOLS_SORT_THREADS,
    null,
    null,
    '-@ 8'
)

processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_MBUFFER_PAIRING_SORTING,
    null,
    null,
    '-m 2G'
)

processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_SAMTOOLS_SORT_BUFFER,
    null,
    null,
    '-m 2000000000'
)

processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_BWA_QUEUE_PARAMETER,
    null,
    null,
    '-q 20'
)

processingOptionService.createOrUpdate(
    OptionName.MAXIMUM_NUMBER_OF_JOBS,
    WORKFLOW_NAME,
    null,
    '100'
)
processingOptionService.createOrUpdate(
    OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK,
    WORKFLOW_NAME,
    null,
    '50'
)
