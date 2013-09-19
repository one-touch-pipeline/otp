import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

plan("QualityAssessmentMergedWorkflow") {
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

// create Quality Assessment jar options for executeBamFileQaAnalysis job
boolean overrideOutput = false
String allChromosomeName = Chromosomes.overallChromosomesLabel()
int minAlignedRecordLength = 36
int minMeanBaseQuality = 25
int mappingQuality = 0
int coverageMappingQualityThreshold = 1
int windowsSize = 1000
int insertSizeCountHistogramBin = 10
boolean testMode = false

String cmd = "qualityAssessment.sh \${processedMergedBamFilePath} \${processedBaiFilePath} \${qualityAssessmentFilePath} \${coverageDataFilePath} \${insertSizeDataFilePath} ${overrideOutput} \${allChromosomeName} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${testMode}"
SeqType seqType = SeqType.findByNameAndLibraryLayout("EXON", "PAIRED")
ctx.processingOptionService.createOrUpdate(
  "qualityMergedAssessment",
  seqType.naturalId,
  null, // defaults to all projects
  cmd,
  "Merged quality assessment Command and parameters template")
