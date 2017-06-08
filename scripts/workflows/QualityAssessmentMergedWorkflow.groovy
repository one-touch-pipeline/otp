import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

String workflowName = "QualityAssessmentMergedWorkflow"

plan(workflowName) {
    start("start", "qualityAssessmentMergedStartJob")
    job("createMergedQaOutputDirectory", "createMergedQaOutputDirectoryJob")
    job("executeMergedBamFileQaAnalysis", "executeMergedBamFileQaAnalysisJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("executeMergedBamFileQaAnalysisWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "executeMergedBamFileQaAnalysis", "__pbsIds")
        inputParameter("__pbsRealm", "executeMergedBamFileQaAnalysis", "__pbsRealm")
    }
    job("mergedQaOutputFileValidation", "mergedQaOutputFileValidationJob")
    job("parseMergedQaStatistics", "parseMergedQaStatisticsJob")
    job("createMergedChromosomeMappingFileJob", "createMergedChromosomeMappingFileJob")
    job("executeMergedMappingFilteringSortingToCoverageTable", "executeMergedMappingFilteringSortingToCoverageTableJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("executeMergedMappingFilteringSortingToCoverageTableWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "executeMergedMappingFilteringSortingToCoverageTable", "__pbsIds")
        inputParameter("__pbsRealm", "executeMergedMappingFilteringSortingToCoverageTable", "__pbsRealm")
    }
    job("mergedMappingFilteringSortingOutputFileValidation", "mergedMappingFilteringSortingOutputFileValidationJob")

    job("createMergedCoveragePlot", "createMergedCoveragePlotJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createMergedCoveragePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createMergedCoveragePlot", "__pbsIds")
        inputParameter("__pbsRealm", "createMergedCoveragePlot", "__pbsRealm")
    }
    job("mergedCoveragePlotValidation", "mergedCoveragePlotValidationJob")
    job("createMergedInsertSizePlot", "createMergedInsertSizePlotJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createMergedInsertSizePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createMergedInsertSizePlot", "__pbsIds")
        inputParameter("__pbsRealm", "createMergedInsertSizePlot", "__pbsRealm")
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

// pbs options for qa.jar for whole genome
String cmd = "qualityAssessment.sh \${processedBamFilePath} \${processedBaiFilePath} \${qualityAssessmentFilePath} \${coverageDataFilePath} \${insertSizeDataFilePath} ${overrideOutput} \${allChromosomeName} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${testMode}"
SeqType seqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.WHOLE_GENOME.seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED)
processingOptionService.createOrUpdate(
    OptionName.PIPELINE_OTP_ALIGNMENT_QUALITY_MERGED_ASSESSMENT,
    seqType.naturalId,
    null, // defaults to all projects
    cmd
)

// pbs options for qa.jar for exome
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
