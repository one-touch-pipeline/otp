import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*


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
ctx.processingOptionService.createOrUpdate(
  "qualityAssessment",
  seqType.naturalId,
  null, // defaults to all projects
  cmd,
  "Quality assessment Command and parameters template for whole genome")

// pbs options for qa.jar for exome
cmd = "qualityAssessment.sh \${processedBamFilePath} \${processedBaiFilePath} \${qualityAssessmentFilePath} \${coverageDataFilePath} \${insertSizeDataFilePath} ${overrideOutput} \${allChromosomeName} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${testMode} \${bedFilePath} \${refGenMetaInfoFilePath}"
seqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED)
ctx.processingOptionService.createOrUpdate(
  "qualityAssessment",
  seqType.naturalId,
  null, // defaults to all projects
  cmd,
  "Quality assessment Command and parameters template for exome")
