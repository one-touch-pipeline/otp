package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import org.springframework.beans.factory.annotation.Autowired
import groovy.text.SimpleTemplateEngine

class ExecuteBamFileQaAnalysisJob extends AbstractJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Autowired
    QualityAssessmentPassService qualityAssessmentPassService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)
        ProcessedBamFile processedBamFile = pass.processedBamFile
        String processedBamFilePath = processedBamFileService.getFilePath(processedBamFile)
        String processedBaiFilePath = processedBamFileService.baiFilePath(processedBamFile)
        String qualityAssessmentFilePath = processedBamFileQaFileService.qualityAssessmentDataFilePath(pass)
        String coverageDataFilePath = processedBamFileQaFileService.coverageDataFilePath(pass)
        String insertSizeDataFilePath = processedBamFileQaFileService.insertSizeDataFilePath(pass)
        String allChromosomeName = Chromosomes.overallChromosomesLabel()
        Project project = processedBamFileService.project(processedBamFile)
        SeqType seqType = processedBamFileService.seqType(processedBamFile)
        String seqTypeNaturalId = seqType.getNaturalId()
        String cmdTemplate = processingOptionService.findOptionAssure("qualityAssessment", seqTypeNaturalId, project)
        Map binding = [
            processedBamFilePath: processedBamFilePath,
            processedBaiFilePath: processedBaiFilePath,
            qualityAssessmentFilePath: qualityAssessmentFilePath,
            coverageDataFilePath: coverageDataFilePath,
            insertSizeDataFilePath: insertSizeDataFilePath,
            allChromosomeName: allChromosomeName
        ]
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        String cmd = engine.createTemplate(cmdTemplate).make(binding).toString()
        cmd += "; chmod 440 ${qualityAssessmentFilePath} ${coverageDataFilePath} ${insertSizeDataFilePath}"
        Realm realm = qualityAssessmentPassService.realmForDataProcessing(pass)
        log.debug cmd
        String pbsID = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
