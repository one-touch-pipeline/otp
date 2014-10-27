import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.dataprocessing.*

plan("ConveyBwaAlignmentWorkflow") {
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
    'conveyBwaNumberOfCores',
    null,
    null,
    '-t 12',
    'number of cores/threads'
)

// create Convey bwa program option for conveyBwaAlignmentJob
ctx.processingOptionService.createOrUpdate(
    'conveyBwaCommand',
    null,
    null,
    'cnybwa-0.6.2',
    'BWA convey command for alignment'
)

// create Convey bwa program quality encoding option for PHRED
ctx.processingOptionService.createOrUpdate(
    'conveyBwaQualityEncoding',
    'PHRED',
    null,
    '',
    'option for quality encoding for PHRED'
)

// create Convey bwa program quality encoding option for ILLUMINA
ctx.processingOptionService.createOrUpdate(
    'conveyBwaQualityEncoding',
    'ILLUMINA',
    null,
    '-I',
    'option for quality encoding for ILLUMINA'
)

// update bwa program option for bwaAlignmentJob
ctx.processingOptionService.createOrUpdate(
    'bwaCommand',
    null,
    null,
    'bwa-0.6.2-tpx',
    'binary BWA command'
)

ctx.processingOptionService.createOrUpdate(
    'samtoolsCommand',
    null,
    null,
    'samtools-0.1.19',
    'samtools (Tools for alignments in the SAM format)'
)

// Create PBS options for bwaPairingAndSortingJob
ctx.processingOptionService.createOrUpdate(
    'PBS_bwaPairingAndSortingJob',
    'DKFZ',
    null,
    '''{
      "-l": {
        "walltime": "50:00:00",
        "nodes": "1:ppn=6:lsdf",
        "mem": "26g"
      }
    }''',
    'BWA Pairing And Sorting Job option for cluster'
)

// Create PBS options for bwaAlignmentJob
ctx.processingOptionService.createOrUpdate(
    'PBS_conveyBwaAlignmentJob',
    'DKFZ',
    null,
    '''{
      "-l": {
        "walltime": "01:00:00",
        "nodes": "1:ppn=12:lsdf",
        "mem": "3g"
      },
      "-q": "convey",
      "-m": "a",
      "-S": "/bin/bash"
    }''',
    'Convey option for cluster'
)

ctx.processingOptionService.createOrUpdate(
    'numberOfSampeThreads',
    null,
    null,
    '-t 8',
    'number of threads to be used in processing with binary bwa sampe'
)

ctx.processingOptionService.createOrUpdate(
    'numberOfSamToolsSortThreads',
    null,
    null,
    '-@ 8',
    'number of threads to be used in processing with samtools sort'
)

ctx.processingOptionService.createOrUpdate(
    'mbufferPairingSorting',
    null,
    null,
    '-m 2G',
    'size of buffer to be used for pairing with binary bwa sampe'
)

ctx.processingOptionService.createOrUpdate(
    'samtoolsSortBuffer',
    null,
    null,
    '-m 2000000000',
    'Buffer for samtools sorting'
)

ctx.processingOptionService.createOrUpdate(
    'bwaQParameter',
    null,
    null,
    '-q 20',
    'quality threshold for "soft" read trimming down to 35bp'
)
