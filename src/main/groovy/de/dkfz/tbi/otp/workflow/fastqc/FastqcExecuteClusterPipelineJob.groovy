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
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteClusterPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

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
        SeqTrack seqTrack = getSeqTrack(workflowStep)
        Realm realm = workflowStep.realm
        FileSystem fileSystem = getFileSystem(workflowStep)
        Path outputDir = fastqcDataFilesService.fastqcOutputDirectory(seqTrack)

        //get data files to be used
        List<DataFile> dataFiles = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)

        //delete existing file in case of restart
        dataFiles.each { DataFile dataFile ->
            Path resultFilePath = fastqcDataFilesService.fastqcOutputPath(dataFile)
            if (Files.exists(resultFilePath)) {
                logService.addSimpleLogEntry(workflowStep, "Delete result file ${resultFilePath}")
                fileService.deleteDirectoryRecursively(resultFilePath)
            }
        }

        //check if fastqc reports are provided and can be copied
        if (canFastQcReportsBeCopied(fileSystem, dataFiles)) {
            //remotely run a script to copy the existing result files
            logService.addSimpleLogEntry(workflowStep, "Copying fastqc reports")
            copyExistingFastqReports(realm, fileSystem, dataFiles, outputDir)
            return []
        }
        //create and return the shell script only (w/o running it)
        logService.addSimpleLogEntry(workflowStep, "Creating cluster scripts")
        return createFastQcClusterScript(dataFiles, outputDir)
    }

    private boolean canFastQcReportsBeCopied(FileSystem fileSystem, List<DataFile> dataFiles) {
        return dataFiles.every { DataFile dataFile ->
            Files.isReadable(fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fileSystem, dataFile))
        }
    }

    private void copyExistingFastqReports(Realm realm, FileSystem fileSystem, List<DataFile> dataFiles, Path outDir) {
        dataFiles.each { dataFile ->
            Path seqCenterFastQcFile = fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fileSystem, dataFile)
            Path seqCenterFastQcFileMd5Sum = fastqcDataFilesService.pathToFastQcResultMd5SumFromSeqCenter(fileSystem, dataFile)
            fileService.ensureFileIsReadableAndNotEmpty(seqCenterFastQcFile)

            String copyAndMd5sumCommand = """|
                |set -e
                |
                |#copy file
                |cd ${seqCenterFastQcFile.parent}
                |md5sum ${seqCenterFastQcFile.fileName} > ${outDir}/${seqCenterFastQcFileMd5Sum.fileName}
                |chmod ${fileService.convertPermissionsToOctalString(fileService.DEFAULT_FILE_PERMISSION)} ${outDir}/${seqCenterFastQcFileMd5Sum.fileName}
                |cp ${seqCenterFastQcFile} ${outDir}
                |chmod ${fileService.convertPermissionsToOctalString(fileService.DEFAULT_FILE_PERMISSION)} ${fastqcDataFilesService.fastqcOutputPath(dataFile)}
                |
                |#check md5sum
                |cd ${outDir}
                |md5sum -c ${seqCenterFastQcFileMd5Sum.fileName}
                |""".stripMargin()

            remoteShellHelper.executeCommandReturnProcessOutput(realm, copyAndMd5sumCommand).assertExitCodeZeroAndStderrEmpty()
            FileService.ensureFileIsReadableAndNotEmpty(seqCenterFastQcFile)
        }
    }

    private List<String> createFastQcClusterScript(List<DataFile> dataFiles, Path outDir) {
        return dataFiles.collect { dataFile ->
            String inputFileName = lsdfFilesService.getFileFinalPath(dataFile)

            String decompressFileCommand = ""
            String deleteDecompressedFileCommand = ""
            FastqcDataFilesService.CompressionFormat usedFormat = FastqcDataFilesService.CompressionFormat.getUsedFormat(inputFileName)
            if (usedFormat) {
                String orgFileName = inputFileName
                inputFileName = fastqcDataFilesService.inputFileNameAdaption(inputFileName)

                decompressFileCommand = "{ ${usedFormat.decompressionCommand} ; } < ${orgFileName} > ${inputFileName}"
                deleteDecompressedFileCommand = "rm -f ${inputFileName}"
            }

            String moduleLoader = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER)
            String fastqcActivation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_ACTIVATION_FASTQC)
            String fastqcCommand = processingOptionService.findOptionAsString(ProcessingOption.OptionName.COMMAND_FASTQC)

            return """|
                |${moduleLoader}
                |${fastqcActivation}
                |${decompressFileCommand}
                |${fastqcCommand} ${inputFileName} --noextract --nogroup -o ${outDir}
                |chmod ${fileService.convertPermissionsToOctalString(fileService.DEFAULT_FILE_PERMISSION)} ${fastqcDataFilesService.fastqcOutputPath(dataFile)}
                |chmod ${fileService.convertPermissionsToOctalString(fileService.DEFAULT_FILE_PERMISSION)} ${fastqcDataFilesService.fastqcHtmlPath(dataFile)}
                |${deleteDecompressedFileCommand}
                |""".stripMargin()
        }
    }
}
