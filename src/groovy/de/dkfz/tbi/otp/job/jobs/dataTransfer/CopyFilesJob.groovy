package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CopyFilesJob extends AbstractJobImpl {

    final String paramName = "__pbsIds"

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        List<DataFile> files = DataFile.findAllByRunAndProjectIsNotNull(run)

        String pbsIds = "1,"
        files.each {DataFile file ->
            String cmd = scriptText(file)
            Realm realm = configService.getRealmDataManagement(file.project)
            String jobId = sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            pbsIds += jobId + ","
        }
        addOutputParameter(paramName, pbsIds)
    }

    private String scriptText(DataFile file) {
        String from = lsdfFilesService.getFileInitialPath(file)
        String to = lsdfFilesService.getFileFinalPath(file)
        return "echo \$HOST;cp ${from} ${to};chmod 440 ${to}"
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
