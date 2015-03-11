package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.jobs.WatchdogJob

import static org.springframework.util.Assert.*
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CopyFilesJob extends AbstractJobImpl {

    final String paramName = "__pbsIds"

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

        List<String> pbsIds = []

        List<ProcessedMergedBamFile> bamFiles = processedMergedBamFilesForRun(run)
        bamFiles.each {
            String cmd = """
mkdir -p -m 2750 ${processedMergedBamFileService.destinationDirectory(it)}
printf "A new lane is currently in progress for this sample.\\nThe merged BAM file will be created/updated as soon as processing is complete.\\n" > ${processedMergedBamFileService.destinationDirectory(it)}/${processedMergedBamFileService.inProgressFileName(it)};
"""
            Realm realm = configService.getRealmDataManagement(it.project)
            executionService.executeCommand(realm, cmd)
        }

        List<DataFile> files = runProcessingService.dataFilesForProcessing(run)
        files.each { DataFile file ->
            Realm realm = configService.getRealmDataManagement(file.project)
            String initialPath = lsdfFilesService.getFileInitialPath(file)
            String finalPath = lsdfFilesService.getFileFinalPath(file)
            if (file.seqTrack.linkedExternally) {
                assert executionService.executeCommand(realm, "ln -s ${initialPath} ${finalPath}; echo \$?") ==~ /0\s*/
            } else {
                String cmd = "echo \$HOST;cp ${initialPath} ${finalPath};chmod 440 ${finalPath}"
                String jobId = executionHelperService.sendScript(realm, cmd, this.getClass().simpleName)
                pbsIds << jobId
            }
        }
        // It is not possible to give an empty list to the watchdog.
        // Therefore a pseudo value is passed and handled in the watchdog.
        if (pbsIds.empty) {
            pbsIds << WatchdogJob.SKIP_WATCHDOG
        }

        addOutputParameter(paramName, pbsIds.join(","))
    }

    /**
     * Returns all {@link ProcessedMergedBamFile}s for samples in the specified {@link Run}.
     */
    public List<ProcessedMergedBamFile> processedMergedBamFilesForRun(Run run) {
        notNull(run, "The run argument must not be null.")
        List<Sample> samples = SeqTrack.findByRun(run)*.sample
        if (!samples) {
            return Collections.emptyList()
        }
        return ProcessedMergedBamFile.withCriteria {
            mergingPass {
                mergingSet {
                    mergingWorkPackage { "in"("sample", samples) }
                }
            }
        }
    }
}
