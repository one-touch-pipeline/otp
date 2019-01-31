package de.dkfz.tbi.otp.job.jobs.cellRanger

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import java.nio.file.*

@Component
@Scope("prototype")
@UseJobLog
class ExecuteCellRangerJob extends AbstractOtpJob implements AutoRestartableJob {


    @Autowired
    CellRangerService cellRangerService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    FileService fileService

    @Autowired
    FileSystemService fileSystemService

    @Autowired
    ProcessingOptionService processingOptionService


    @Override
    protected NextAction maybeSubmit() throws Throwable {
        final SingleCellBamFile singleCellBamFile = getProcessParameterObject()
        final Realm realm = singleCellBamFile.project.realm

        prepareInputStructure(singleCellBamFile)

        String jobScript = createScript(singleCellBamFile)

        clusterJobSchedulerService.executeJob(realm, jobScript)

        return NextAction.WAIT_FOR_CLUSTER_JOBS
    }

    private void prepareInputStructure(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)

        Path workDirectory = fileSystem.getPath(singleCellBamFile.workDirectory.path)
        fileService.createDirectoryRecursively(workDirectory)
        fileService.setPermission(workDirectory, FileService.OWNER_DIRECTORY_PERMISSION)

        cellRangerService.deleteOutputDirectoryStructureIfExists(singleCellBamFile)
        cellRangerService.createInputDirectoryStructure(singleCellBamFile)
    }


    @Override
    protected void validate() throws Throwable {
        final SingleCellBamFile singleCellBamFile = getProcessParameterObject()

        cellRangerService.validateFilesExistsInResultDirectory(singleCellBamFile)
    }


    private String createScript(SingleCellBamFile singleCellBamFile) {
        String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
        String moduleLoadPrefix = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ENABLE_MODULE)

        String script = """\
            ${moduleLoader}
            ${moduleLoadPrefix} ${singleCellBamFile.mergingWorkPackage.config.programVersion}

            cd ${singleCellBamFile.workDirectory}
            ${createCommand(singleCellBamFile)}

            ${createMd5SumCommand(singleCellBamFile)}
            """

        return script
    }


    private String createCommand(SingleCellBamFile singleCellBamFile) {
        Map<String, String> parameters = cellRangerService.createCellRangerParameters(singleCellBamFile)

        StringBuilder builder = new StringBuilder(1000)
        builder << "cellranger count"
        parameters.each { String key, String value ->
            builder << " " << key << "=" << value
        }
        builder << " --disable-ui"
        return builder.toString()
    }

    private String createMd5SumCommand(SingleCellBamFile singleCellBamFile) {
        File resultDirectory = singleCellBamFile.resultDirectory
        File bamFile = new File(resultDirectory, SingleCellBamFile.ORIGINAL_BAM_FILE_NAME)
        File md5sum = new File(resultDirectory, SingleCellBamFile.ORIGINAL_BAM_MD5SUM_FILE_NAME)

        return "md5sum ${bamFile.path} | sed -e 's#  ${bamFile.path}##' > ${md5sum.path}"
    }
}
