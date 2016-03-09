package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired

class CreateCoveragePlotJob extends AbstractJobImpl {

    @Autowired
    PbsService pbsService

    @Autowired
    QualityAssessmentPassService qualityAssessmentPassService

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)
        String mappedFilteredSortedCoverageDataFilePath = processedBamFileQaFileService.mappedFilteredSortedCoverageDataFilePath(pass)
        String plotFilePath = processedBamFileQaFileService.coveragePlotFilePath(pass)
        String cmd = "coveragePlot.sh ${mappedFilteredSortedCoverageDataFilePath} ${plotFilePath}; chmod 440 ${plotFilePath}"
        Realm realm = qualityAssessmentPassService.realmForDataProcessing(pass)
        String pbsID = pbsService.executeJob(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
