import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "QualityAssessmentMergedWorkflow"

plan(workflowName) {
    start("start", "qualityAssessmentMergedStartJob")
    job("createMergedQaOutputDirectory", "createMergedQaOutputDirectoryJob")
    job("executeMergedBamFileQaAnalysis", "executeMergedBamFileQaAnalysisJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }
    job("executeMergedBamFileQaAnalysisWatchdog", "myPBSWatchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "executeMergedBamFileQaAnalysis", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "executeMergedBamFileQaAnalysis", JobParameterKeys.REALM)
    }
    job("mergedQaOutputFileValidation", "mergedQaOutputFileValidationJob")
    job("parseMergedQaStatistics", "parseMergedQaStatisticsJob")
    job("createMergedChromosomeMappingFileJob", "createMergedChromosomeMappingFileJob")
    job("executeMergedMappingFilteringSortingToCoverageTable", "executeMergedMappingFilteringSortingToCoverageTableJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }
    job("executeMergedMappingFilteringSortingToCoverageTableWatchdog", "myPBSWatchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "executeMergedMappingFilteringSortingToCoverageTable", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "executeMergedMappingFilteringSortingToCoverageTable", JobParameterKeys.REALM)
    }
    job("mergedMappingFilteringSortingOutputFileValidation", "mergedMappingFilteringSortingOutputFileValidationJob")

    job("createMergedCoveragePlot", "createMergedCoveragePlotJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }
    job("createMergedCoveragePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "createMergedCoveragePlot", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "createMergedCoveragePlot", JobParameterKeys.REALM)
    }
    job("mergedCoveragePlotValidation", "mergedCoveragePlotValidationJob")
    job("createMergedInsertSizePlot", "createMergedInsertSizePlotJob") {
        outputParameter(JobParameterKeys.JOB_ID_LIST)
        outputParameter(JobParameterKeys.REALM)
    }
    job("createMergedInsertSizePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter(JobParameterKeys.JOB_ID_LIST, "createMergedInsertSizePlot", JobParameterKeys.JOB_ID_LIST)
        inputParameter(JobParameterKeys.REALM, "createMergedInsertSizePlot", JobParameterKeys.REALM)
    }
    job("mergedInsertSizePlotValidation", "mergedInsertSizePlotValidationJob")
    job("assignMergedQaFlag", "assignMergedQaFlagJob")
}

ProcessingOptionService processingOptionService = ctx.processingOptionService

// number of all MergedQA workflows which can be executed in parallel
println processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS, workflowName, null, '60')

// number of slots which are reserved only for FastTrack Workflows
println processingOptionService.createOrUpdate(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, workflowName, null, '30')



// creates options to be used by the wf to run the qa.jar

boolean overrideOutput = false
int minAlignedRecordLength = 36
int minMeanBaseQuality = 25
int mappingQuality = 0
int coverageMappingQualityThreshold = 1
int windowsSize = 1000
int insertSizeCountHistogramBin = 10
boolean testMode = false

// options for qa.jar for whole genome
String cmd = "qualityAssessment.sh \${processedBamFilePath} \${processedBaiFilePath} \${qualityAssessmentFilePath} \${coverageDataFilePath} \${insertSizeDataFilePath} ${overrideOutput} \${allChromosomeName} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${testMode}"
SeqType seqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME.seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED)
processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_QUALITY_MERGED_ASSESSMENT,
    seqType.naturalId,
    null, // defaults to all projects
    cmd
)

// options for qa.jar for exome
cmd = "qualityAssessment.sh \${processedBamFilePath} \${processedBaiFilePath} \${qualityAssessmentFilePath} \${coverageDataFilePath} \${insertSizeDataFilePath} ${overrideOutput} \${allChromosomeName} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${testMode} \${bedFilePath} \${refGenMetaInfoFilePath}"
seqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED)
processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_QUALITY_MERGED_ASSESSMENT,
    seqType.naturalId,
    null, // defaults to all projects
    cmd
)


println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${ExecuteMergedBamFileQaAnalysisJob.class.simpleName}",
        null,
        '{"WALLTIME":"PT100H","MEMORY":"15g"}',
)

println processingOptionService.createOrUpdate(
        OptionName.CLUSTER_SUBMISSIONS_OPTION,
        "${ExecuteMergedMappingFilteringSortingToCoverageTableJob.simpleName}",
        null,
        '{"MEMORY":"15g","CORES":"6"}',
)
