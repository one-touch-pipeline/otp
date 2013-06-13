package de.dkfz.tbi.otp.job.jobs.qualityAssessment

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.dataprocessing.*
//import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.AbstractJobImpl
import org.springframework.beans.factory.annotation.Autowired

class CreateInsertSizePlotJob extends AbstractJobImpl {

//    @Autowired
//    ExecutionService executionService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    ProcessedBamFileQaFileService processedBamFileQaFileService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile processedBamFile = ProcessedBamFile.get(Long.parseLong(getProcessParameterValue()))
        Realm realm = processedBamFileService.realm(processedBamFile)
        String dataFilePath = processedBamFileQaFileService.insertSizeDataFilePath(processedBamFile)
        String plotFilePath = processedBamFileQaFileService.insertSizePlotFilePath(processedBamFile)
        // TODO check how to pass the right parameters..
        String chromosome = "ALL"
        // TODO Does anybody knows how this parameters with spaces in the middle can be passed ? they are labels for the plot but if they have spaces..
        // they will be interpreted as diferent ones..
//        String cmd = "insertSizePlot.sh ${dataFilePath} ${chromosome} Insert_size_distribution_${chromosome} Insert_size frequency ${plotFilePath}"
//        String pbsID = sendScript(realm, cmd)
        String mainTitle = "Insert size distribution for all chromosomes"
        String yAxisLabel = "Frequency"
        String xAxisLabel = "Insert size"
        int plotWidth = 800
        int plotHeight = 600
        // TODO have to try to use ' instead of \"   (needed because title may have spaces..)
        String cmd = "insertSizePlot.sh ${dataFilePath} ${chromosome} \"${mainTitle}\" \"${xAxisLabel}\" \"${yAxisLabel}\" ${plotFilePath} ${plotWidth} ${plotHeight}"
        String pbsID = executionHelperService.sendScript(realm, cmd)
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
