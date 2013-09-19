package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired

class CreateMergedCoveragePlotJob extends AbstractJobImpl {

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
        String mappedFilteredSortedCoverageDataFilePath = processedMergedBamFileQaFileService.mappedFilteredSortedCoverageDataFilePath(pass)
        String plotFilePath = processedMergedBamFileQaFileService.coveragePlotFilePath(pass)
        String cmd = "coveragePlot.sh ${mappedFilteredSortedCoverageDataFilePath} ${plotFilePath}; chmod 440 ${plotFilePath}"
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
        String pbsID = executionHelperService.sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
