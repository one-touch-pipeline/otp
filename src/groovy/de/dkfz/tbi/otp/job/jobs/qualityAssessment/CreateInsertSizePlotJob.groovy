package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired

class CreateInsertSizePlotJob extends AbstractJobImpl {

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    QualityAssessmentPassService qualityAssessmentPassService

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)
        Realm realm = qualityAssessmentPassService.realmForDataProcessing(pass)
        String dataFilePath = processedBamFileQaFileService.insertSizeDataFilePath(pass)
        String plotFilePath = processedBamFileQaFileService.insertSizePlotFilePath(pass)
        String allChromosomeName = Chromosomes.overallChromosomesLabel()
        String cmd = "insertSizePlot.sh '${dataFilePath}' '${allChromosomeName}' '${plotFilePath}'; chmod 440 ${plotFilePath}"
        log.debug cmd
        String pbsID = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
