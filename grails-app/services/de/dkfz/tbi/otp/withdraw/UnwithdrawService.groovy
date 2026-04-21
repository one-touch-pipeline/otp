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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataAllWellFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.infrastructure.fastqc.FastqcLinkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.filestore.FilestoreService

import java.nio.file.*

@Transactional
class UnwithdrawService {

    AbstractBamFileService abstractBamFileService
    ConfigService configService
    FastqcLinkFileService fastqcLinkFileService
    FileService fileService
    FileSystemService fileSystemService
    ProcessingOptionService processingOptionService
    WithdrawAnalysisService withdrawAnalysisService
    RawSequenceDataAllWellFileService rawSequenceDataAllWellFileService
    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    RawSequenceDataViewFileService rawSequenceDataViewFileService
    FilestoreService filestoreService

    @Autowired
    List<AbstractWithdrawBamFileService<?>> withdrawBamFileServices

    @CompileDynamic
    void unwithdrawSeqTracks(UnwithdrawStateHolder unwithdrawStateHolder) {
        unwithdrawStateHolder.seqTracksWithComment.each { seqTrackWithComment ->
            unwithdrawStateHolder.summary << "\n\nUnwithdraw ${seqTrackWithComment.seqTrack}"
            RawSequenceFile.findAllBySeqTrack(seqTrackWithComment.seqTrack).each {
                unwithdrawRawSequenceFiles(it, seqTrackWithComment.comment, unwithdrawStateHolder)
            }
        }

        if (unwithdrawStateHolder.nonExistingRawSequenceFiles) {
            unwithdrawStateHolder.summary << "\n\nWarning: The following files could not be unwithdraw due to missing files:"
            unwithdrawStateHolder.nonExistingRawSequenceFiles.each { errorMessage ->
                unwithdrawStateHolder.summary << "  - ${errorMessage}"
            }
        }
    }

    @CompileDynamic
    @SuppressWarnings(['AbcMetric', 'CyclomaticComplexity'])
    private void unwithdrawRawSequenceFiles(final RawSequenceFile rawSequenceFile, String comment, UnwithdrawStateHolder unwithdrawStateHolder) {
        unwithdrawStateHolder.summary << "Unwithdrawing RawSequenceFile: ${rawSequenceFile}: ${rawSequenceFile.withdrawnComment}"

        Path sourceFile = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
        if (!sourceFile) {
            String errorMessage = "Cannot unwithdraw ${rawSequenceFile.id}: could not establish file path"
            unwithdrawStateHolder.nonExistingRawSequenceFiles.add(errorMessage)
            return
        }
        if (!Files.exists(sourceFile)) {
            String errorMessage = "Cannot unwithdraw ${rawSequenceFile.id}: source file does not exist: ${sourceFile}"
            unwithdrawStateHolder.nonExistingRawSequenceFiles.add(errorMessage)
            return
        }

        // Set group on the view by PID link (no need to create since withdraw no longer deletes it)
        unwithdrawStateHolder.pathsToChangeGroup.put(rawSequenceDataViewFileService.getFilePath(rawSequenceFile).toString(), rawSequenceFile.project.unixGroup)
        // Set group on single cell well link if it exists
        if (rawSequenceFile.seqType.singleCell && rawSequenceFile.seqTrack.singleCellWellLabel) {
            unwithdrawStateHolder.pathsToChangeGroup.put(rawSequenceDataAllWellFileService.getFilePath(rawSequenceFile).toString(),
                    rawSequenceFile.project.unixGroup)
        }
        FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))
        List<Path> files = [
                rawSequenceDataWorkFileService.getFilePath(rawSequenceFile),
                rawSequenceDataWorkFileService.getMd5sumPath(rawSequenceFile),
        ]
        if (fastqcProcessedFile) {
            files.addAll([
                    fastqcLinkFileService.fastqcOutputPath(fastqcProcessedFile),
                    fastqcLinkFileService.fastqcOutputMd5sumPath(fastqcProcessedFile),
                    fastqcLinkFileService.fastqcHtmlPath(fastqcProcessedFile),
            ])
        }
        files.findAll { path ->
            path && Files.exists(path)
        }.collect { filePath ->
            unwithdrawStateHolder.pathsToChangeGroup.put(filePath.toString(), rawSequenceFile.project.unixGroup)
        }

        boolean fastqIsOldWorkflow = !rawSequenceFile.seqTrack.workflowArtefact?.producedBy?.workFolder
        boolean fastqcIsOldWorkflow = fastqcProcessedFile && !fastqcProcessedFile.workflowArtefact?.producedBy?.workFolder

        List<Path> fastqFilePermissionPaths = []

        if (fastqIsOldWorkflow) {
            // Add the main FASTQ file and its MD5 sum
            Path fastqFilePath = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
            Path md5sumFilePath = rawSequenceDataWorkFileService.getMd5sumPath(rawSequenceFile)

            if (fastqFilePath && Files.exists(fastqFilePath) && Files.isRegularFile(fastqFilePath)) {
                fastqFilePermissionPaths.add(fastqFilePath)
            }
            if (md5sumFilePath && Files.exists(md5sumFilePath) && Files.isRegularFile(md5sumFilePath)) {
                fastqFilePermissionPaths.add(md5sumFilePath)
            }
        }

        if (fastqcIsOldWorkflow) {
            // Add FastQC files if they exist (only actual files, not directories)
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
            unwithdrawStateHolder.pathsToChangePermissions.put(filePath.toString(), "444")
        }

        rawSequenceFile.withdrawnDate = null
        if (!rawSequenceFile.withdrawnComment?.contains(comment)) {
            rawSequenceFile.withdrawnComment = "${rawSequenceFile.withdrawnComment ? "${rawSequenceFile.withdrawnComment}\n" : ""}${comment}"
        }
        rawSequenceFile.fileWithdrawn = false
        rawSequenceFile.save(flush: true)
    }

    @CompileDynamic
    void unwithdrawBamFiles(UnwithdrawStateHolder withdrawStateHolder) {
        Map<AbstractWithdrawBamFileService, List<AbstractBamFile>> bamFileMap = withdrawBamFileServices.collectEntries {
            [(it), it.collectObjects(withdrawStateHolder.seqTracks).unique().findAll { AbstractBamFile bamFile ->
                bamFile.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED &&
                        !bamFile.containedSeqTracks.any { it.withdrawn } &&
                        (Files.exists(abstractBamFileService.getBaseDirectory(bamFile).resolve(bamFile.bamFileName)) ||
                                (bamFile.workflowArtefact?.producedBy?.workFolder &&
                                        Files.exists(filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy))))
            },]
        }
        withdrawStateHolder.bamFiles = bamFileMap.values().flatten().unique()
        if (withdrawStateHolder.bamFiles.size() > 0) {
            withdrawStateHolder.bamFiles.each {
                withdrawStateHolder.summary << "Unwithdrawing BAM file: ${it}" as String
            }
        }

        bamFileMap.each {
            withdrawStateHolder.pathsToChangeGroup.putAll(it.value.collectEntries { bamFile ->
                it.key.collectPaths([bamFile]).collectEntries { String path ->
                    [(path): bamFile.project.unixGroup]
                }
            })
            it.key.unwithdrawObjects(it.value)
        }
    }

    void unwithdrawAnalysis(UnwithdrawStateHolder withdrawStateHolder) {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        List<BamFilePairAnalysis> bamFilePairAnalysisList = withdrawAnalysisService.collectObjects(withdrawStateHolder.bamFiles).unique()
        bamFilePairAnalysisList = bamFilePairAnalysisList.findAll {
            it.processingState == AnalysisProcessingStates.FINISHED &&
                    !it.sampleType1BamFile.withdrawn && !it.sampleType2BamFile.withdrawn &&
                    withdrawAnalysisService.collectPaths([it]).every { path -> Files.exists(fileSystem.getPath(path)) }
        }

        withdrawStateHolder.pathsToChangeGroup.putAll(bamFilePairAnalysisList.collectEntries {
            List<String> collectedPaths = withdrawAnalysisService.collectPaths([it])
            collectedPaths ? [collectedPaths.first().toString(), it.project.unixGroup] : null
        })
        if (bamFilePairAnalysisList.size() > 0) {
            withdrawStateHolder.summary << ("Unwithdrawing analysis result: ${bamFilePairAnalysisList}" as String)
            withdrawAnalysisService.unwithdrawObjects(bamFilePairAnalysisList)
        } else {
            withdrawStateHolder.summary << 'Unwithdrawing analysis result: Nothing to do'
        }
    }

    void writeBashScript(UnwithdrawStateHolder withdrawStateHolder) {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        Path outputFile = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('withdrawn').resolve(withdrawStateHolder.scriptFileName)

        fileService.deleteDirectoryRecursively(outputFile) // delete file if already exists
        String unixGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
        fileService.createFileWithContent(outputFile, withdrawStateHolder.script.join('\n'),
                unixGroup, FileService.OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION)

        withdrawStateHolder.summary << "\nScript Path:"
        withdrawStateHolder.summary << outputFile.toString()
    }

    void createBashScript(UnwithdrawStateHolder withdrawStateHolder) {
        withdrawStateHolder.script << "\n#change group for links, files and directories"
        withdrawStateHolder.pathsToChangeGroup.each { path, group ->
            withdrawStateHolder.script << ("chgrp --no-dereference --recursive --verbose " +
                    LocalShellHelper.shellEscape(group) + " " + LocalShellHelper.shellEscape(path))
        }

        withdrawStateHolder.script << "\n#restore file permissions to 444 for unwithdrawn FASTQ files"
        withdrawStateHolder.pathsToChangePermissions.each { filePath, permission ->
            withdrawStateHolder.script << ("chmod " + LocalShellHelper.shellEscape(permission) + " " + LocalShellHelper.shellEscape(filePath))
        }

        withdrawStateHolder.script << "\necho script has run till end\n"
    }
}

class UnwithdrawStateHolder {

    List<SeqTrackWithComment> seqTracksWithComment = []

    List<String> summary = []

    Map<String, String> pathsToChangeGroup = [:]

    Map<String, String> pathsToChangePermissions = [:]

    List<AbstractBamFile> bamFiles = []

    List<String> script = []

    String scriptFileName

    List<String> nonExistingRawSequenceFiles = []

    List<SeqTrack> getSeqTracks() {
        return seqTracksWithComment*.seqTrack
    }
}
