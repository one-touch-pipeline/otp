package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.dataprocessing.*
//import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired

class CreateCoveragePlotJob extends AbstractJobImpl {

    @Autowired
    ExecutionHelperService executionHelperService
//    ExecutionService executionService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(Long.parseLong(getProcessParameterValue()))
        Realm realm = processedBamFileService.realm(processedBamFile)
        String sortedAndFilteredCoverageDataFilePath = processedBamFileQaFileService.sortedCoverageDataFilePath(processedBamFile)
        //TODO The countType could be also be passed "base" this value is not involved in any calculations for the plot,
        // and is just for label presentation, so it is totally dependent on how the data was generated..
        // so either should be a parameter also for the QA jar or we assume that the jar is only generating read counts and we have a flexible plot script
        String countType = "read"
        int windowSize = 1000
        String plotFilePath = processedBamFileQaFileService.coveragePlotFilePath(processedBamFile)
        String cmd = "coveragePlot.sh ${sortedAndFilteredCoverageDataFilePath} ${countType} ${windowSize} ${plotFilePath}"
        String pbsID = executionHelperService.sendScript(realm, cmd)
//        String cmd = "coveragePlot.sh ${sortedAndFilteredCoverageDataFilePath} read 1000 ${plotFilePath}"
//        String pbsID = sendScript(realm, cmd)
        addOutputParameter("__pbsIds", pbsID)
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

//    private String sendScript(Realm realm, String text) {
//        String pbsResponse = executionService.executeJob(realm, text)
//        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
//        if (extractedPbsIds.size() != 1) {
//            log.debug "Number of PBS jobs is = ${extractedPbsIds.size()}"
//        }
//        return extractedPbsIds.get(0)
//    }
}
