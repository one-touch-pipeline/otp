package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.utils.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*

class ExecuteMergedMappingFilteringSortingToCoverageTableJob extends AbstractJobImpl {

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    QualityAssessmentMergedPassService qualityAssessmentMergedPassService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentMergedPass pass = QualityAssessmentMergedPass.get(passId)
        String chromosomeMappingFilePath = processedMergedBamFileQaFileService.chromosomeMappingFilePath(pass)
        String genomeCoverageFilePath = processedMergedBamFileQaFileService.coverageDataFilePath(pass)
        String mappedFilteredSortedCoverageDataFilePath = processedMergedBamFileQaFileService.mappedFilteredSortedCoverageDataFilePath(pass)
        String overwriteOutput = false
        List parameters = [
            chromosomeMappingFilePath,
            genomeCoverageFilePath,
            mappedFilteredSortedCoverageDataFilePath,
            overwriteOutput
        ]
        String application = "coverageFileMFS.sh"
        String cmd = application
        parameters.each { String parameter ->
            cmd += " ${parameter}"
        }
        cmd += "; chmod 440 ${mappedFilteredSortedCoverageDataFilePath}"
        Realm realm = qualityAssessmentMergedPassService.realmForDataProcessing(pass)
        String jobId = clusterJobSchedulerService.executeJob(realm, cmd)
        addOutputParameter(JobParameterKeys.JOB_ID_LIST, jobId)
        addOutputParameter(JobParameterKeys.REALM, realm.id.toString())
    }
}
