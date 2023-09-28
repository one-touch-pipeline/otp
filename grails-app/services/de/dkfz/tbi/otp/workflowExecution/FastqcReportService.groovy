/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper

import java.nio.file.Files
import java.nio.file.Path

@Slf4j
@Transactional
class FastqcReportService {

    FastqcDataFilesService fastqcDataFilesService
    FileService fileService

    @Autowired
    RemoteShellHelper remoteShellHelper

    boolean canFastqcReportsBeCopied(List<FastqcProcessedFile> fastqcProcessedFiles) {
        return fastqcProcessedFiles && fastqcProcessedFiles.every { FastqcProcessedFile fastqcProcessedFile ->
            Files.isReadable(fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fastqcProcessedFile))
        }
    }

    /**
     * @deprecated old system
     */
    @Deprecated
    void copyExistingFastqcReports(List<FastqcProcessedFile> fastqcProcessedFiles, Path outDir) {
        fastqcProcessedFiles.each { FastqcProcessedFile fastqcProcessedFile ->
            Path seqCenterFastQcFile = fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fastqcProcessedFile)
            Path seqCenterFastQcFileMd5Sum = fastqcDataFilesService.pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile)
            Path fastqcOutputPath = fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile)
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
                |chmod ${permission} ${fastqcOutputPath}
                |
                |#check md5sum
                |cd ${outDir}
                |md5sum -c ${seqCenterFastQcFileMd5Sum.fileName}
                |""".stripMargin()

            remoteShellHelper.executeCommandReturnProcessOutput(copyAndMd5sumCommand).assertExitCodeZeroAndStderrEmpty()
            fileService.ensureFileIsReadableAndNotEmpty(fastqcOutputPath)
        }
    }

    void copyExistingFastqcReportsNewSystem(List<FastqcProcessedFile> fastqcProcessedFiles) {
        fastqcProcessedFiles.each { FastqcProcessedFile fastqcProcessedFile ->
            Path seqCenterFastQcFile = fastqcDataFilesService.pathToFastQcResultFromSeqCenter(fastqcProcessedFile)
            Path seqCenterFastQcFileMd5Sum = fastqcDataFilesService.pathToFastQcResultMd5SumFromSeqCenter(fastqcProcessedFile)
            fileService.ensureFileIsReadableAndNotEmpty(seqCenterFastQcFile)

            Path realPath = fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile, PathOption.REAL_PATH)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(realPath.parent)

            String copyAndMd5sumCommand = """|
                |set -e
                |
                |#copy file
                |cd ${seqCenterFastQcFile.parent}
                |md5sum ${seqCenterFastQcFile.fileName} > ${realPath.parent}/${seqCenterFastQcFileMd5Sum.fileName}
                |cp ${seqCenterFastQcFile} ${realPath}
                |
                |#check md5sum
                |cd ${realPath.parent}
                |md5sum -c ${seqCenterFastQcFileMd5Sum.fileName}
                |""".stripMargin()

            remoteShellHelper.executeCommandReturnProcessOutput(copyAndMd5sumCommand).assertExitCodeZeroAndStderrEmpty()
            fileService.ensureFileIsReadableAndNotEmpty(realPath)
        }
    }
}
