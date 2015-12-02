import static de.dkfz.tbi.otp.job.processing.PbsOptionMergingService.PBS_PREFIX

import de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged.*
import de.dkfz.tbi.otp.job.processing.PbsOptionMergingService;
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster


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
  "qualityMergedAssessment",
  seqType.naturalId,
  null, // defaults to all projects
  cmd,
  "Quality assessment Command and parameters template for whole genome")

// pbs options for qa.jar for exome
cmd = "qualityAssessment.sh \${processedBamFilePath} \${processedBaiFilePath} \${qualityAssessmentFilePath} \${coverageDataFilePath} \${insertSizeDataFilePath} ${overrideOutput} \${allChromosomeName} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${testMode} \${bedFilePath} \${refGenMetaInfoFilePath}"
seqType = SeqType.findByNameAndLibraryLayout(SeqTypeNames.EXOME.seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED)
ctx.processingOptionService.createOrUpdate(
  "qualityMergedAssessment",
  seqType.naturalId,
  null, // defaults to all projects
  cmd,
  "Quality assessment Command and parameters template for exome")


println ctx.processingOptionService.createOrUpdate(
    PbsOptionMergingService.PBS_PREFIX + ExecuteMergedBamFileQaAnalysisJob.class.simpleName,
    "DKFZ",
    null,
    '{"-l": {walltime: "100:00:00", mem: "15g"}}',
    "time for merged QA"
)

println ctx.processingOptionService.createOrUpdate(
    "${PBS_PREFIX}${ExecuteMergedMappingFilteringSortingToCoverageTableJob.simpleName}",
    Cluster.DKFZ.toString(),
    null,
    '{"-l": {nodes: "1:ppn=6", mem: "15g"}}',
    ''
)
