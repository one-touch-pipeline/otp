/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.jobs.cellRanger

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.Paths

@Component
@Scope("prototype")
@Slf4j
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
        final SingleCellBamFile singleCellBamFile = processParameterObject
        final Realm realm = singleCellBamFile.project.realm

        prepareInputStructure(singleCellBamFile)

        String jobScript = createScript(singleCellBamFile)

        clusterJobSchedulerService.executeJob(realm, jobScript)

        return NextAction.WAIT_FOR_CLUSTER_JOBS
    }

    private void prepareInputStructure(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        String unixGroup = singleCellBamFile.project.unixGroup

        Path workDirectory = fileSystem.getPath(singleCellBamFile.workDirectory.path)
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(workDirectory, singleCellBamFile.realm, unixGroup)
        fileService.setPermissionViaBash(workDirectory, singleCellBamFile.project.realm, FileService.OWNER_DIRECTORY_PERMISSION_STRING)

        cellRangerService.deleteOutputDirectoryStructureIfExists(singleCellBamFile)
        cellRangerService.createInputDirectoryStructure(singleCellBamFile)
    }

    @Override
    protected void validate() throws Throwable {
        final SingleCellBamFile singleCellBamFile = processParameterObject

        cellRangerService.validateFilesExistsInResultDirectory(singleCellBamFile)
    }

    private String createScript(SingleCellBamFile singleCellBamFile) {
        String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
        String moduleLoadPrefix = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ENABLE_MODULE)
        String command = createCommand(singleCellBamFile)

        String script = """\
            ${moduleLoader}
            ${moduleLoadPrefix} ${singleCellBamFile.mergingWorkPackage.config.programVersion}

            cd ${singleCellBamFile.workDirectory}
            ${command}

            echo \"${command}\" > ${Paths.get(singleCellBamFile.resultDirectory.toString(), "${singleCellBamFile.singleCellSampleName}_" +
                "${SingleCellBamFile.CELL_RANGER_COMMAND_FILE_NAME}").toAbsolutePath()}

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
