/*
 * Copyright 2011-2026 The OTP authors
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
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.infrastructure.fastqc.FastqcLinkFileService
import de.dkfz.tbi.otp.infrastructure.fastqc.FastqcWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.DeletionService
import de.dkfz.tbi.otp.utils.exceptions.FileAccessForProjectNotAllowedException

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
    FastqcLinkFileService fastqcLinkFileService
    FastqcWorkFileService fastqcWorkFileService
    FileService fileService
    FileSystemService fileSystemService
    ProcessingOptionService processingOptionService
    FilestoreService filestoreService
    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    RawSequenceDataViewFileService rawSequenceDataViewFileService
    RawSequenceDataAllWellFileService rawSequenceDataAllWellFileService

    @Autowired
    List<AbstractWithdrawBamFileService<?>> withdrawBamFileServices
    WithdrawAnalysisService withdrawAnalysisService
    WithdrawDisplayDomainService withdrawDisplayDomainService

    @CompileDynamic
    void createOverviewSummary(WithdrawStateHolder withdrawStateHolder) {
        List summary = withdrawStateHolder.summary
        summary << "Withdraw summary"
        summary << "\n"
        summary << TRIM_LINE
        summary << ("Withdrawing ${withdrawStateHolder.seqTracksWithComments.size()} lanes" as String)
        withdrawStateHolder.seqTracksWithComments.each {
            summary << ("  - ${withdrawDisplayDomainService.seqTrackInfo(it.seqTrack)}\twith comment: \'" + it.comment + "\'" as String)
        }
        summary << "\n"
        summary << TRIM_LINE
        String deleteOrWithdrawBamFile = withdrawStateHolder.deleteBamFile ? 'Deleting' : 'Withdrawing'
        summary << ("${deleteOrWithdrawBamFile} ${withdrawStateHolder.bamFiles.unique().size()} bam file(s)" as String)
        withdrawStateHolder.bamFiles.unique().each {
            summary << ("  - ${withdrawDisplayDomainService.bamFileInfo(it)}" as String)
        }
        summary << "\n"
        summary << TRIM_LINE
        String deleteOrWithdrawAnalysis = withdrawStateHolder.deleteBamFile || withdrawStateHolder.deleteAnalysis ? 'Deleting' : 'Withdrawing'
        summary << ("${deleteOrWithdrawAnalysis} ${withdrawStateHolder.analysis.unique().size()} analysis" as String)
        withdrawStateHolder.analysis.unique().each {
            summary << ("  - ${withdrawDisplayDomainService.analysisInfo(it)}" as String)
        }
        summary << "\n"
        summary << TRIM_LINE
    }

    void checkArchivedProject(WithdrawStateHolder withdrawStateHolder) {
        if (withdrawStateHolder.seqTracks.any { it.project.state == Project.State.ARCHIVED }) {
            throw new FileAccessForProjectNotAllowedException("Project is archived, withdrawal is not allowed")
        }
        if (withdrawStateHolder.seqTracks.any { it.project.state == Project.State.DELETED }) {
            throw new FileAccessForProjectNotAllowedException("Project is deleted, withdrawal is not allowed")
        }
    }

    @CompileDynamic
    void checkNonExistingRawSequenceFiles(WithdrawStateHolder withdrawStateHolder) {
        List<RawSequenceFile> nonExistingRawSequenceFiles = RawSequenceFile.findAllBySeqTrackInListAndFileExists(withdrawStateHolder.seqTracks, false)

        if (nonExistingRawSequenceFiles) {
            List<String> nonExistingData = nonExistingRawSequenceFiles.collect {
                withdrawDisplayDomainService.rawSequenceFileInfo(it)
            }.sort()

            if (withdrawStateHolder.stopOnMissingFiles) {
                throw new WithdrawnException(
                        "Stopped since ${nonExistingRawSequenceFiles.size()} datafiles do not exist on the file system:\n" +
                                nonExistingData.join('\n')
                )
            }

            withdrawStateHolder.summary << "\n${nonExistingRawSequenceFiles.size()} datafiles found that do not exist on the file system:"
            withdrawStateHolder.summary.addAll(nonExistingData)
            withdrawStateHolder.summary << "\n${NOTE_IGNORE_MISSING_FILES}"
        }
    }

    @CompileDynamic
    void checkForAlreadyWithdrawnRawSequenceFiles(WithdrawStateHolder withdrawStateHolder) {
        List<RawSequenceFile> withdrawnRawSequenceFiles = RawSequenceFile.findAllBySeqTrackInListAndFileWithdrawn(withdrawStateHolder.seqTracks, true)

        if (withdrawnRawSequenceFiles) {
            List<String> withdrawnData = withdrawnRawSequenceFiles.collect {
                withdrawDisplayDomainService.rawSequenceFileInfo(it, true)
            }.sort()

            if (withdrawStateHolder.stopOnAlreadyWithdrawnData) {
                throw new WithdrawnException("Stopped since ${withdrawnRawSequenceFiles.size()} datafiles are already withdrawn:\n${withdrawnData.join('\n')}")
            }
            withdrawStateHolder.summary << "\n${withdrawnRawSequenceFiles.size()} datafiles are already withdrawn: "
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

    void handleBamFiles(WithdrawStateHolder withdrawStateHolder, Map<AbstractWithdrawBamFileService, List<AbstractBamFile>> bamFileMap) {
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

    @CompileDynamic
    @SuppressWarnings(['AbcMetric', 'CyclomaticComplexity'])
    void handleRawSequenceFiles(WithdrawStateHolder withdrawStateHolder) {
        List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.findAllBySeqTrackInListAndFileWithdrawn(
                withdrawStateHolder.seqTracksWithComments*.seqTrack,
                false
        )
        Map<SeqTrack, String> commentBySeqTrack = withdrawStateHolder.seqTracksWithComments.collectEntries {
            [(it.seqTrack): it.comment]
        }
        rawSequenceFiles.each { RawSequenceFile rawSequenceFile ->
            FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))

            rawSequenceFile.fileWithdrawn = true
            rawSequenceFile.withdrawnDate = new Date()
            rawSequenceFile.withdrawnComment = commentBySeqTrack[rawSequenceFile.seqTrack]
            rawSequenceFile.save(flush: true)

            List<Path> filePaths = []
            Path fastqFilePath = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
            Path md5sumFilePath = rawSequenceDataWorkFileService.getMd5sumPath(rawSequenceFile)
            filePaths.add(fastqFilePath)
            filePaths.add(md5sumFilePath)
            if (fastqcProcessedFile) {
                // add the symbolic link
                filePaths.add(fastqcLinkFileService.getDirectoryPath(fastqcProcessedFile))
                // add the uuid folder, which the link pointed to
                filePaths.add(fastqcWorkFileService.getDirectoryPath(fastqcProcessedFile))
            }

            filePaths.unique().findAll { path ->
                return Files.exists(path)
            }.collect { existingPath ->
                withdrawStateHolder.pathsToChangeGroup << existingPath.toString()
            }

            boolean fastqIsOldWorkflow = !rawSequenceFile.seqTrack.workflowArtefact?.producedBy?.workFolder
            boolean fastqcIsOldWorkflow = fastqcProcessedFile && !fastqcProcessedFile.workflowArtefact?.producedBy?.workFolder

            List<Path> fastqFilePermissionPaths = []

            if (fastqIsOldWorkflow) {
                if (fastqFilePath && Files.exists(fastqFilePath) && Files.isRegularFile(fastqFilePath)) {
                    fastqFilePermissionPaths.add(fastqFilePath)
                }
                if (md5sumFilePath && Files.exists(md5sumFilePath) && Files.isRegularFile(md5sumFilePath)) {
                    fastqFilePermissionPaths.add(md5sumFilePath)
                }
            }

            if (fastqcIsOldWorkflow) {
                Path fastqcZipFile = fastqcLinkFileService.fastqcOutputPath(fastqcProcessedFile)
                Path fastqcMd5File = fastqcLinkFileService.fastqcOutputMd5sumPath(fastqcProcessedFile)
                Path fastqcHtmlFile = fastqcLinkFileService.fastqcHtmlPath(fastqcProcessedFile)

                if (fastqcZipFile && Files.exists(fastqcZipFile) && Files.isRegularFile(fastqcZipFile)) {
                    fastqFilePermissionPaths.add(fastqcZipFile)
                }
                if (fastqcMd5File && Files.exists(fastqcMd5File) && Files.isRegularFile(fastqcMd5File)) {
                    fastqFilePermissionPaths.add(fastqcMd5File)
                }
                if (fastqcHtmlFile && Files.exists(fastqcHtmlFile) && Files.isRegularFile(fastqcHtmlFile)) {
                    fastqFilePermissionPaths.add(fastqcHtmlFile)
                }
            }

            fastqFilePermissionPaths.unique().each { filePath ->
                withdrawStateHolder.pathsToChangePermissions << filePath.toString()
            }

            withdrawStateHolder.pathsToDelete << rawSequenceDataViewFileService.getFilePath(rawSequenceFile).toString()
            if (rawSequenceFile.seqType.singleCell && rawSequenceFile.seqTrack.singleCellWellLabel) {
                withdrawStateHolder.pathsToDelete << rawSequenceDataAllWellFileService.getFilePath(rawSequenceFile).toString()
            }

            MergingWorkPackage.withCriteria {
                seqTracks {
                    eq('id', rawSequenceFile.seqTrack.id)
                }
            }.each { MergingWorkPackage mergingWorkPackage ->
                mergingWorkPackage.seqTracks.remove(rawSequenceFile.seqTrack)
                mergingWorkPackage.save(flush: true)
            }
        }
    }

    Path createAndWriteBashScript(WithdrawStateHolder withdrawStateHolder) {
        FileSystem fileSystem = withdrawStateHolder.remoteFileSystem
        Path outputFile = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('withdrawn').resolve(withdrawStateHolder.fileName)

        String script = createBashScript(withdrawStateHolder)

        fileService.deleteDirectoryRecursively(outputFile) // delete file if it already exists
        String unixGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
        fileService.createFileWithContent(outputFile, script, unixGroup, FileService.OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION)

        withdrawStateHolder.summary << "\nScript Path:"
        withdrawStateHolder.summary << outputFile.toString()

        return outputFile
    }

    String createBashScript(WithdrawStateHolder withdrawStateHolder) {
        String withdrawnGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP)

        List<String> script = [
                FileService.BASH_HEADER,
        ]

        script << "\n#Deleted links, files and directories"
        withdrawStateHolder.pathsToDelete.each {
            script << ("rm --recursive --force --verbose ${it}" as String)
        }

        script << "\n#change group for links, files and directories"
        withdrawStateHolder.pathsToChangeGroup.each {
            script << ("chgrp --recursive --verbose ${withdrawnGroup} ${it}" as String)
        }

        script << "\n#change file permissions to 440 for withdrawn FASTQ files"
        withdrawStateHolder.pathsToChangePermissions.each {
            script << ("chmod 440 ${it}" as String)
        }

        script << "\necho the script has run to the end\n"

        return script.join('\n')
    }
}
