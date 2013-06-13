package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import org.springframework.beans.factory.annotation.Autowired

class ExecuteBamFileQaAnalysisJob extends AbstractJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(Long.parseLong(getProcessParameterValue()))
        String processedBamFilePath = processedBamFileService.getFilePath(processedBamFile)
        // TODO Replace by apropriate service...
        String processedBaiFilePath = "${processedBamFilePath}.bai"
        String coverageDataFilePath = processedBamFileQaFileService.coverageDataFilePath(processedBamFile)
        String qualityAssessmentFilePath = processedBamFileQaFileService.qualityAssessmentDataFilePath(processedBamFile)
        String insertSizeDataFilePath = processedBamFileQaFileService.insertSizeDataFilePath(processedBamFile)
        boolean override = false
        int minAlignedRecordLength = 36
        int minMeanBaseQuality = 25
        int mappingQuality = 0
        int coverageMappingQualityThreshold = 1
        int windowsSize = 1000
        int insertSizeCountHistogramBin = 10
        boolean test = true

//        Realm realm = configService.getRealmDataProcessing(processedBamFile.alignmentPass.seqTrack.sample.individual.project)
        // TODO Maybe these numerical parameters should go to options framework or at least be written as variable names to be more clear..
//        String cmd = "QualityAssessment.sh ${processedBamFilePath} ${qualityAssessmentFilePath} ${coverageDataFilePath} ${insertSizeDataFilePath} 36 25 0 1 1000 10"
        String cmd = "QualityAssessment.sh ${processedBamFilePath} ${processedBaiFilePath} ${qualityAssessmentFilePath} ${coverageDataFilePath} ${insertSizeDataFilePath} ${override} ${minAlignedRecordLength} ${minMeanBaseQuality} ${mappingQuality} ${coverageMappingQualityThreshold} ${windowsSize} ${insertSizeCountHistogramBin} ${test}"
//        Realm realm = configService.getRealmDataProcessing(processedBamFile.alignmentPass.seqTrack.sample.individual.project)

        Realm realm = ProcessedBamFileService.realm(processedBamFile)


        String pbsID = sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String sendScript(Realm realm, String text) {
        String pbsResponse = executionService.executeJob(realm, text)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            log.debug "Number of PBS jobs is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }
}
