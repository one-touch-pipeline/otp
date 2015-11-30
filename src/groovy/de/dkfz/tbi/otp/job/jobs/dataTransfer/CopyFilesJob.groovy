package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.jobs.WatchdogJob
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys

import static org.springframework.util.Assert.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CopyFilesJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ConfigService configService

    @Autowired
    RunProcessingService runProcessingService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        Run run = Run.get(Long.parseLong(getProcessParameterValue()))
        notNull(run, "The run id must not be invalid.")

        List<String> pbsIds = []
        List<String> realmIds = []

        List<DataFile> files = runProcessingService.dataFilesForProcessing(run)
        files.each { DataFile file ->
            Realm realm = configService.getRealmDataManagement(file.project)
            String initialPath = lsdfFilesService.getFileInitialPath(file)
            String finalPath = lsdfFilesService.getFileFinalPath(file)
            //Aligned bam files are not directly connected with seq track but indirectly via alignment log
            //The following code ensures that they always are copied
            if (file.seqTrack?.linkedExternally) {
                assert executionService.executeCommand(realm, "ln -s ${initialPath} ${finalPath}; echo \$?") ==~ /0\s*/
            } else {
                String cmd = "echo \$HOST;cp ${initialPath} ${finalPath};chmod 440 ${finalPath}"
                String jobId = executionHelperService.sendScript(realm, cmd)
                pbsIds << jobId
                realmIds << realm.id.toString()
            }
        }
        // It is not possible to give an empty list to the watchdog.
        // Therefore a pseudo value is passed and handled in the watchdog.
        if (pbsIds.empty) {
            pbsIds << WatchdogJob.SKIP_WATCHDOG
            realmIds << WatchdogJob.SKIP_WATCHDOG
        }

        addOutputParameter(JobParameterKeys.PBS_ID_LIST, pbsIds.join(","))
        addOutputParameter(JobParameterKeys.REALM, realmIds.join(","))
    }
}
