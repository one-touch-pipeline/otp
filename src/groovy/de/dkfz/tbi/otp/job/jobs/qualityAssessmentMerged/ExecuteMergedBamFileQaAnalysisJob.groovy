package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import org.springframework.beans.factory.annotation.Autowired
import groovy.text.SimpleTemplateEngine

class ExecuteMergedBamFileQaAnalysisJob extends AbstractJobImpl {

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        ProcessedMergedBamFile processedMergedBamFile = pass.processedMergedBamFile
        String processedMergedBamFilePath = processedMergedBamFileService.filePath(processedMergedBamFile)
        String processedMergedBaiFilePath = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        String qualityAssessmentFilePath = processedMergedBamFileQaFileService.qualityAssessmentDataFilePath(pass)
        String coverageDataFilePath = processedMergedBamFileQaFileService.coverageDataFilePath(pass)
        String insertSizeDataFilePath = processedMergedBamFileQaFileService.insertSizeDataFilePath(pass)
        String allChromosomeName = Chromosomes.overallChromosomesLabel()
        Project project = processedMergedBamFileService.project(processedMergedBamFile)
        SeqType seqType = processedMergedBamFileService.seqType(processedMergedBamFile)
        String seqTypeNaturalId = seqType.getNaturalId()
        String cmdTemplate = processingOptionService.findOptionAssure("qualityAssessment", seqTypeNaturalId, project)
        Map binding = [
            processedBamFilePath: processedMergedBamFilePath,
            processedBaiFilePath: processedMergedBaiFilePath,
            qualityAssessmentFilePath: qualityAssessmentFilePath,
            coverageDataFilePath: coverageDataFilePath,
            insertSizeDataFilePath: insertSizeDataFilePath,
            allChromosomeName: allChromosomeName
        ]
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        String cmd = engine.createTemplate(cmdTemplate).make(binding).toString()
        cmd += "; chmod 440 ${qualityAssessmentFilePath} ${coverageDataFilePath} ${insertSizeDataFilePath}"
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
        String pbsID = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
