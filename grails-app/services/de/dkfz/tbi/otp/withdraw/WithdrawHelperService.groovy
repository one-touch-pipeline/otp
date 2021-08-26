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
package de.dkfz.tbi.otp.withdraw

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.*

/**
 * Internal service for simplify {@link WithdrawService}.
 *
 * The service should used outside.
 */
@PreAuthorize("hasRole('ROLE_OPERATOR')")
@Transactional
class WithdrawHelperService {

    static final String TRIM_LINE = "----------------------------------------"
    static final String NOTE_IGNORE_MISSING_FILES = "It is ignored in request"
    static final String NOTE_IGNORE_ALREADY_WITHDRAWN = "Ignore withdrawn data files"

    ConfigService configService
    DeletionService deletionService
    FastqcDataFilesService fastqcDataFilesService
    FileService fileService
    FileSystemService fileSystemService
    LsdfFilesService lsdfFilesService
    ProcessingOptionService processingOptionService
    @Autowired
    List<WithdrawBamFileService<?>> withdrawBamFileServices
    WithdrawAnalysisService withdrawAnalysisService
    WithdrawDisplayDomainService withdrawDisplayDomainService

    void createOverviewSummary(WithdrawStateHolder withdrawStateHolder) {
        List summary = withdrawStateHolder.summary
        summary << "Withdraw summary"
        summary << "\nUsed comment"
        summary << withdrawStateHolder.withdrawnComment
        summary << "\n"
        summary << TRIM_LINE
        summary << "Withdrawing ${withdrawStateHolder.seqTracks.size()} lanes"
        withdrawStateHolder.seqTracks.each {
            summary << "  - ${withdrawDisplayDomainService.seqTrackInfo(it)}"
        }
        summary << "\n"
        summary << TRIM_LINE
        String deleteOrWithdrawBamFile = withdrawStateHolder.deleteBamFile ? 'Deleting' : 'Withdrawing'
        summary << "${deleteOrWithdrawBamFile} ${withdrawStateHolder.mergedBamFiles.size()} bam files"
        withdrawStateHolder.mergedBamFiles.each {
            summary << "  - ${withdrawDisplayDomainService.bamFileInfo(it)}"
        }
        summary << "\n"
        summary << TRIM_LINE
        String deleteOrWithdrawAnalysis = withdrawStateHolder.deleteBamFile || withdrawStateHolder.deleteAnalysis ? 'Deleting' : 'Withdrawing'
        summary << "${deleteOrWithdrawAnalysis} ${withdrawStateHolder.analysis.size()} analysis"
        withdrawStateHolder.analysis.each {
            summary << "  - ${withdrawDisplayDomainService.analysisInfo(it)}"
        }
        summary << "\n"
        summary << TRIM_LINE
    }

    void checkNonExistingDataFiles(WithdrawStateHolder withdrawStateHolder) {
        List<DataFile> nonExistingDataFiles = DataFile.findAllBySeqTrackInListAndFileExists(withdrawStateHolder.seqTracks, false)

        if (nonExistingDataFiles) {
            List<String> nonExistingData = nonExistingDataFiles.collect {
                withdrawDisplayDomainService.dataFileInfo(it)
            }.sort()

            if (withdrawStateHolder.stopOnMissingFiles) {
                throw new WithdrawnException("Stop, since ${nonExistingDataFiles.size()} datafiles are not existing on file system:\n" +
                        nonExistingData.join('\n'))
            }

            withdrawStateHolder.summary << "\n${nonExistingDataFiles.size()} datafiles not existing on file system found:"
            withdrawStateHolder.summary.addAll(nonExistingData)
            withdrawStateHolder.summary << "\n${NOTE_IGNORE_MISSING_FILES}"
        }
    }

    void checkForAlreadyWithdrawnDatafiles(WithdrawStateHolder withdrawStateHolder) {
        List<DataFile> withdrawnDataFiles = DataFile.findAllBySeqTrackInListAndFileWithdrawn(withdrawStateHolder.seqTracks, true)

        if (withdrawnDataFiles) {
            List<String> withdrawnData = withdrawnDataFiles.collect {
                withdrawDisplayDomainService.dataFileInfo(it, true)
            }.sort()

            if (withdrawStateHolder.stopOnAlreadyWithdrawnData) {
                throw new WithdrawnException("Stop, since ${withdrawnDataFiles.size()} datafiles are already withdrawn:\n${withdrawnData.join('\n')}")
            }
            withdrawStateHolder.summary << "\n${withdrawnDataFiles.size()} datafiles are already withdrawn: "
            withdrawStateHolder.summary.addAll(withdrawnData)
            withdrawStateHolder.summary << "\n${NOTE_IGNORE_ALREADY_WITHDRAWN}"
        }
    }

    void handleAnalysis(WithdrawStateHolder withdrawStateHolder) {
        if (withdrawStateHolder.deleteAnalysis || withdrawStateHolder.deleteBamFile) {
            withdrawStateHolder.pathsToDelete.addAll(withdrawAnalysisService.collectPaths(withdrawStateHolder.analysis))
            withdrawAnalysisService.deleteObjects(withdrawStateHolder.analysis)
        } else {
            withdrawStateHolder.pathsToChangeGroup.addAll(withdrawAnalysisService.collectPaths(withdrawStateHolder.analysis))
            withdrawAnalysisService.withdrawObjects(withdrawStateHolder.analysis)
        }
    }

    void handleBamFiles(WithdrawStateHolder withdrawStateHolder, Map<WithdrawBamFileService, List<AbstractMergedBamFile>> bamFileMap) {
        if (withdrawStateHolder.deleteBamFile) {
            bamFileMap.each {
                withdrawStateHolder.pathsToDelete.addAll(it.key.collectPaths(it.value))
                it.key.deleteObjects(it.value)
            }
        } else {
            bamFileMap.each {
                withdrawStateHolder.pathsToChangeGroup.addAll(it.key.collectPaths(it.value))
                it.key.withdrawObjects(it.value)
            }
        }
    }

    void handleDataFiles(WithdrawStateHolder withdrawStateHolder) {
        FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm
        List<DataFile> dataFiles = DataFile.findAllBySeqTrackInListAndFileWithdrawn(withdrawStateHolder.seqTracks, false)
        dataFiles.each { DataFile dataFile ->
            dataFile.fileWithdrawn = true
            dataFile.withdrawnDate = new Date()
            dataFile.withdrawnComment = withdrawStateHolder.withdrawnComment
            dataFile.save(flush: true)

            List<Path> filePaths = []
            filePaths.add( lsdfFilesService.getFileFinalPathAsPath(dataFile, fileSystem))
            filePaths.add( lsdfFilesService.getFileMd5sumFinalPathAsPath(dataFile, fileSystem))
            filePaths.add(fastqcDataFilesService.fastqcOutputPath(dataFile, fileSystem))
            filePaths.add(fastqcDataFilesService.fastqcHtmlPath(dataFile, fileSystem))
            filePaths.add(fastqcDataFilesService.fastqcOutputMd5sumPath(dataFile, fileSystem))

            filePaths.findAll { path ->
                return Files.exists(path)
            }.collect { existingPath ->
                withdrawStateHolder.pathsToChangeGroup << existingPath.toString()
            }

            withdrawStateHolder.pathsToDelete << lsdfFilesService.getFileViewByPidPath(dataFile)
            if (dataFile.seqType.singleCell && dataFile.seqTrack.singleCellWellLabel) {
                withdrawStateHolder.pathsToDelete << lsdfFilesService.getWellAllFileViewByPidPath(dataFile)
            }
        }
    }

    Path createAndWriteBashScript(WithdrawStateHolder withdrawStateHolder) {
        FileSystem fileSystem = withdrawStateHolder.remoteFileSystem
        Path outputFile = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('withdrawn').resolve(withdrawStateHolder.fileName)

        String script = createBashScript(withdrawStateHolder)

        fileService.deleteDirectoryRecursively(outputFile) //delete file if already exists
        fileService.createFileWithContentOnDefaultRealm(outputFile, script, FileService.OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION)

        withdrawStateHolder.summary << "\nDeletionPath:"
        withdrawStateHolder.summary << outputFile

        return outputFile
    }

    String createBashScript(WithdrawStateHolder withdrawStateHolder) {
        String withdrawnGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP)

        List<String> script = [
                FileService.BASH_HEADER,
        ]

        script << "\n#Deleted links, files and directories"
        withdrawStateHolder.pathsToDelete.each {
            script << "rm --recursive --force --verbose ${it}"
        }

        script << "\n#change group for links, files and directories"
        withdrawStateHolder.pathsToChangeGroup.each {
            script << "chgrp --recursive --verbose ${withdrawnGroup} ${it}"
        }

        script << "\necho script has run till end\n"

        return script.join('\n')
    }
}
