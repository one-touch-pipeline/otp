import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

plan("QualityAssessmentWorkflow") {
    start("start", "qualityAssessmentStartJob")
    job("createQaOutputDirectory", "createQaOutputDirectoryJob")
    job("executeBamFileQaAnalysis", "executeBamFileQaAnalysisJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("executeBamFileQaAnalysisWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "executeBamFileQaAnalysis", "__pbsIds")
        inputParameter("__pbsRealm", "executeBamFileQaAnalysis", "__pbsRealm")
    }
    job("qaOutputFileValidation", "qaOutputFileValidationJob")
    job("parseQaStatistics", "parseQaStatisticsJob")
    job("createChromosomeMappingFile", "createChromosomeMappingFileJob")
    job("executeMappingFilteringSortingToCoverageTable", "executeMappingFilteringSortingToCoverageTableJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("executeMappingFilteringSortingToCoverageTableWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "executeMappingFilteringSortingToCoverageTable", "__pbsIds")
        inputParameter("__pbsRealm", "executeMappingFilteringSortingToCoverageTable", "__pbsRealm")
    }
    job("mappingFilteringSortingOutputFileValidation", "mappingFilteringSortingOutputFileValidationJob")

    job("createCoveragePlot", "createCoveragePlotJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createCoveragePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createCoveragePlot", "__pbsIds")
        inputParameter("__pbsRealm", "createCoveragePlot", "__pbsRealm")
    }
    job("coveragePlotValidation", "coveragePlotValidationJob")
    job("createInsertSizePlot", "createInsertSizePlotJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createInsertSizePlotWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createInsertSizePlot", "__pbsIds")
        inputParameter("__pbsRealm", "createInsertSizePlot", "__pbsRealm")
    }
    job("insertSizePlotValidation", "insertSizePlotValidationJob")
    job("assignQaFlag", "assignQaFlagJob")
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
boolean testMode = true // TODO better to ask again CO Group

String cmd = "qualityAssessment.sh \${processedBamFilePath} \${processedBaiFilePath} \${qualityAssessmentFilePath} \${coverageDataFilePath} \${insertSizeDataFilePath} ${overrideOutput} \${allChromosomeName} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${testMode}"
SeqType seqType = SeqType.findByNameAndLibraryLayout("WHOLE_GENOME", "PAIRED")
ctx.processingOptionService.createOrUpdate(
  "qualityAssessment",
  seqType.naturalId,
  null, // defaults to all projects
  cmd,
  "Quality assessment Command and parameters template")
