package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class CreateMergedInsertSizePlotJob  extends AbstractJobImpl {

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Override
    void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
        String dataFilePath = processedMergedBamFileQaFileService.insertSizeDataFilePath(pass)
        String plotFilePath = processedMergedBamFileQaFileService.insertSizePlotFilePath(pass)
        String allChromosomeName = Chromosomes.overallChromosomesLabel()
        String cmd = "insertSizePlot.sh '${dataFilePath}' '${allChromosomeName}' '${plotFilePath}'; chmod 440 ${plotFilePath}"
        String jobId = clusterJobSchedulerService.executeJob(realm, cmd)
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
    }
}
