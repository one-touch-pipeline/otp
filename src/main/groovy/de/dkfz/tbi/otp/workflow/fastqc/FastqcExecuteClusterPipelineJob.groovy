/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.fastqc

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteClusterPipelineJob
import de.dkfz.tbi.otp.workflow.shared.NoWorkflowVersionSpecifiedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

import java.nio.file.Files
import java.nio.file.Path

@Component
@Slf4j
class FastqcExecuteClusterPipelineJob extends AbstractExecuteClusterPipelineJob implements FastqcShared {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    protected List<String> createScripts(WorkflowStep workflowStep) {
        Realm realm = workflowStep.realm
        Path outputDir = getFileSystem(workflowStep).getPath(workflowStep.workflowRun.workDirectory)

        //delete existing output directory in case of restart
        if (Files.exists(outputDir)) {
            logService.addSimpleLogEntry(workflowStep, "Deleting output directory ${outputDir}")
            fileService.deleteDirectoryRecursively(outputDir)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(outputDir, workflowStep.workflowRun.project.realm,
                    workflowStep.workflowRun.project.unixGroup,)
        }

        List<FastqcProcessedFile> fastqcProcessedFiles = getFastqcProcessedFiles(workflowStep)

        //check if fastqc reports are provided and can be copied
        if (canFastQcReportsBeCopied(fastqcProcessedFiles)) {
            //remotely run a script to copy the existing result files
            logService.addSimpleLogEntry(workflowStep, "Copying fastqc reports")
            copyExistingFastqReports(realm, fastqcProcessedFiles, outputDir)
            return []
        }
        //create and return the shell script only (w/o running it)
        logService.addSimpleLogEntry(workflowStep, "Creating cluster scripts")
        return createFastQcClusterScript(fastqcProcessedFiles, outputDir, workflowStep)
    }

    private boolean canFastQcReportsBeCopied(List<FastqcProcessedFile> fastqcProcessedFiles) {
        return fastqcProcessedFiles.every { FastqcProcessedFile fastqcProcessedFile ->
            Files.isReadable(fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fastqcProcessedFile))
        }
    }

    private void copyExistingFastqReports(Realm realm, List<FastqcProcessedFile> fastqcProcessedFiles, Path outDir) {
        fastqcProcessedFiles.each { FastqcProcessedFile fastqcProcessedFile ->
            Path seqCenterFastQcFile = fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fastqcProcessedFile)
            Path seqCenterFastQcFileMd5Sum = fastqcDataFilesService.pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile)
            fileService.ensureFileIsReadableAndNotEmpty(seqCenterFastQcFile)

            String permission = fileService.convertPermissionsToOctalString(FileService.DEFAULT_FILE_PERMISSION)

            String copyAndMd5sumCommand = """|
                |set -e
                |
                |#copy file
                |cd ${seqCenterFastQcFile.parent}
                |md5sum ${seqCenterFastQcFile.fileName} > ${outDir}/${seqCenterFastQcFileMd5Sum.fileName}
                |chmod ${permission} ${outDir}/${seqCenterFastQcFileMd5Sum.fileName}
                |cp ${seqCenterFastQcFile} ${outDir}
                |chmod ${permission} ${fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile)}
                |
                |#check md5sum
                |cd ${outDir}
                |md5sum -c ${seqCenterFastQcFileMd5Sum.fileName}
                |""".stripMargin()

            remoteShellHelper.executeCommandReturnProcessOutput(realm, copyAndMd5sumCommand).assertExitCodeZeroAndStderrEmpty()
            FileService.ensureFileIsReadableAndNotEmpty(seqCenterFastQcFile)
        }
    }

    private List<String> createFastQcClusterScript(List<FastqcProcessedFile> fastqcProcessedFiles, Path outDir, WorkflowStep workflowStep) {
        String permission = fileService.convertPermissionsToOctalString(FileService.DEFAULT_FILE_PERMISSION)

        return fastqcProcessedFiles.collect { FastqcProcessedFile fastqcProcessedFile ->
            String inputFileName = lsdfFilesService.getFileFinalPath(fastqcProcessedFile.dataFile)

            String decompressFileCommand = ""
            String deleteDecompressedFileCommand = ""
            FastqcDataFilesService.CompressionFormat usedFormat = FastqcDataFilesService.CompressionFormat.getUsedFormat(inputFileName)
            if (usedFormat) {
                String orgFileName = inputFileName
                inputFileName = fastqcDataFilesService.inputFileNameAdaption(inputFileName)

                decompressFileCommand = "{ ${usedFormat.decompressionCommand} ; } < ${orgFileName} > ${inputFileName}"
                deleteDecompressedFileCommand = "rm -f ${inputFileName}"
            }

            WorkflowVersion workflowVersion = workflowStep.workflowRun.workflowVersion
            if (!workflowVersion) {
                throw new NoWorkflowVersionSpecifiedException("For project '${workflowStep.workflowRun.project}' no fastqc workflow version is specified")
            }
            String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
            String fastqcActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ENABLE_MODULE) + ' fastqc/' +
                    workflowVersion.workflowVersion
            String fastqcCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_FASTQC)

            return """|
                |${moduleLoader }
                |${fastqcActivation }
                |${decompressFileCommand}
                |${fastqcCommand} ${inputFileName} --noextract --nogroup -o ${outDir}
                |chmod ${permission} ${fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile)}
                |chmod ${permission} ${fastqcDataFilesService.fastqcHtmlPath(fastqcProcessedFile)}
                |${deleteDecompressedFileCommand}
                |""".stripMargin()
        }
    }
}
