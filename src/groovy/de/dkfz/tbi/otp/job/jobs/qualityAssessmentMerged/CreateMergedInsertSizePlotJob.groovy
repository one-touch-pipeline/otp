package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired

class CreateMergedInsertSizePlotJob  extends AbstractJobImpl {

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
        String dataFilePath = processedMergedBamFileQaFileService.insertSizeDataFilePath(pass)
        String plotFilePath = processedMergedBamFileQaFileService.insertSizePlotFilePath(pass)
        String allChromosomeName = Chromosomes.overallChromosomesLabel()
        String cmd = "insertSizePlot.sh '${dataFilePath}' '${allChromosomeName}' '${plotFilePath}'; chmod 440 ${plotFilePath}"
        log.debug cmd
        String pbsID = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
