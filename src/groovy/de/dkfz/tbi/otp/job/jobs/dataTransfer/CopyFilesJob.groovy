package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CopyFilesJob extends AbstractJobImpl {

    final String paramName = "__pbsIds"

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ConfigService configService

    @Autowired
    RunProcessingService runProcessingService

    @Autowired
    ExecutionHelperService executionHelperService

    @Override
    public void execute() throws Exception {
        Run run = Run.get(Long.parseLong(getProcessParameterValue()))
        List<DataFile> files = runProcessingService.dataFilesForProcessing(run)

        List<String> pbsIds = []
        files.each { DataFile file ->
            String cmd = scriptText(file)
            Realm realm = configService.getRealmDataManagement(file.project)
            String jobId = executionHelperService.sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            pbsIds << jobId
        }
        addOutputParameter(paramName, pbsIds.join(","))
    }

    private String scriptText(DataFile file) {
        String from = lsdfFilesService.getFileInitialPath(file)
        String to = lsdfFilesService.getFileFinalPath(file)
        return "echo \$HOST;cp ${from} ${to};chmod 440 ${to}"
    }
}
