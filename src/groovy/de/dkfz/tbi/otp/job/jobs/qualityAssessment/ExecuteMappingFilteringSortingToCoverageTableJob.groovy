package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class ExecuteMappingFilteringSortingToCoverageTableJob extends AbstractJobImpl {

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Autowired
    PbsService pbsService

    @Autowired
    QualityAssessmentPassService qualityAssessmentPassService

    @Override
    public void execute() throws Exception {
        long passId = getProcessParameterValue() as long
        QualityAssessmentPass pass = QualityAssessmentPass.get(passId)
        String chromosomeMappingFilePath = processedBamFileQaFileService.chromosomeMappingFilePath(pass)
        String genomeCoverageFilePath = processedBamFileQaFileService.coverageDataFilePath(pass)
        String mappedFilteredSortedCoverageDataFilePath = processedBamFileQaFileService.mappedFilteredSortedCoverageDataFilePath(pass)
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
        Realm realm = qualityAssessmentPassService.realmForDataProcessing(pass)
        String pbsID = pbsService.executeJob(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
