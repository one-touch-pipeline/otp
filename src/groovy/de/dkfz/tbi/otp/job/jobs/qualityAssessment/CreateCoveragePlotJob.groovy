package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired

class CreateCoveragePlotJob extends AbstractJobImpl {

    @Autowired
    ExecutionService executionService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(Long.parseLong(getProcessParameterValue()))
        Realm realm = processedBamFileService.realm(processedBamFile)
        String sortedAndFilteredCoverageDataFilePath = processedBamFileQaFileService.sortedCoverageDataFilePath(processedBamFile)
        String plotFilePath = processedBamFileQaFileService.coveragePlotFilePath(processedBamFile)
        String cmd = "coveragePlot.sh ${sortedAndFilteredCoverageDataFilePath} read 1000 ${plotFilePath}"
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
