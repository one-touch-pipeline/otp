package de.dkfz.tbi.otp.job.jobs.dataTransfer

import static org.springframework.util.Assert.*
import java.util.List;
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService;
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

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

    @Override
    public void execute() throws Exception {
        Run run = Run.get(Long.parseLong(getProcessParameterValue()))

        List<String> pbsIds = []

        List<ProcessedMergedBamFile> bamFiles = processedMergedBamFilesForRun(run)
        bamFiles.each {
            String cmd = """
mkdir -p -m 0750 ${processedMergedBamFileService.destinationDirectory(it)}
echo "A new lane is currently in progress for this sample.\\nThe merged BAM file will be created/updated as soon as processing is complete." > ${processedMergedBamFileService.destinationDirectory(it)}/${processedMergedBamFileService.inProgressFileName(it)};
"""
            Realm realm = configService.getRealmDataManagement(it.project)
            String jobId = executionHelperService.sendScript(realm, cmd)
            log.debug "Job ${jobId} submitted to PBS"
            pbsIds << jobId
        }

        List<DataFile> files = runProcessingService.dataFilesForProcessing(run)
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
