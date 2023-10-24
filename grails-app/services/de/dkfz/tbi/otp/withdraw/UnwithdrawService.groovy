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
package de.dkfz.tbi.otp.withdraw

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.*

@CompileDynamic
@Transactional
class UnwithdrawService {

    AbstractBamFileService abstractBamFileService
    ConfigService configService
    FastqcDataFilesService fastqcDataFilesService
    FileService fileService
    FileSystemService fileSystemService
    LsdfFilesService lsdfFilesService
    WithdrawAnalysisService withdrawAnalysisService

    @Autowired
    List<AbstractWithdrawBamFileService<?>> withdrawBamFileServices

    void unwithdrawSeqTracks(UnwithdrawStateHolder unwithdrawStateHolder) {
        unwithdrawStateHolder.seqTracksWithComment.each { seqTrackWithComment ->
            unwithdrawStateHolder.summary << "\n\nUnwithdraw ${seqTrackWithComment.seqTrack}"
            RawSequenceFile.findAllBySeqTrack(seqTrackWithComment.seqTrack).each {
                unwithdrawRawSequenceFiles(it, seqTrackWithComment.comment, unwithdrawStateHolder)
            }
        }
    }

    private void unwithdrawRawSequenceFiles(final RawSequenceFile rawSequenceFile, String comment, UnwithdrawStateHolder unwithdrawStateHolder) {
        unwithdrawStateHolder.summary << "Unwithdrawing RawSequenceFile: ${rawSequenceFile}: ${rawSequenceFile.withdrawnComment}"
        unwithdrawStateHolder.linksToCreate.put(lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile),
                lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile))
        unwithdrawStateHolder.pathsToChangeGroup.put(lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile).toString(), rawSequenceFile.project.unixGroup)
        FastqcProcessedFile fastqcProcessedFile = CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile))
        List<Path> files = [
                lsdfFilesService.getFileFinalPathAsPath(rawSequenceFile),
                lsdfFilesService.getFileMd5sumFinalPathAsPath(rawSequenceFile),
        ]
        if (fastqcProcessedFile) {
            files.addAll([
                    fastqcDataFilesService.fastqcOutputPath(fastqcProcessedFile),
                    fastqcDataFilesService.fastqcOutputMd5sumPath(fastqcProcessedFile),
                    fastqcDataFilesService.fastqcHtmlPath(fastqcProcessedFile),
            ])
        }
        files.findAll { path ->
            Files.exists(path)
        }.collect { filePath ->
            unwithdrawStateHolder.pathsToChangeGroup.put(filePath.toString(), rawSequenceFile.project.unixGroup)
        }
        rawSequenceFile.withdrawnDate = null
        if (!rawSequenceFile.withdrawnComment?.contains(comment)) {
            rawSequenceFile.withdrawnComment = "${rawSequenceFile.withdrawnComment ? "${rawSequenceFile.withdrawnComment}\n" : ""}${comment}"
        }
        rawSequenceFile.fileWithdrawn = false
        rawSequenceFile.save(flush: true)
    }

    void unwithdrawBamFiles(UnwithdrawStateHolder withdrawStateHolder) {
        Map<AbstractWithdrawBamFileService, List<AbstractBamFile>> bamFileMap = withdrawBamFileServices.collectEntries {
            [(it), it.collectObjects(withdrawStateHolder.seqTracks).unique().findAll { bamFile ->
                bamFile.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED &&
                        !bamFile.containedSeqTracks.any { it.withdrawn } &&
                        Files.exists(abstractBamFileService.getBaseDirectory(bamFile).resolve(bamFile.bamFileName))
            },]
        }
        withdrawStateHolder.bamFiles = bamFileMap.values().flatten().unique()
        if (withdrawStateHolder.bamFiles.size() > 0) {
            withdrawStateHolder.bamFiles.each {
                withdrawStateHolder.summary << "Unwithdrawing BAM file: ${it}"
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
        List<BamFilePairAnalysis> analysis = withdrawAnalysisService.collectObjects(withdrawStateHolder.bamFiles).unique()
        analysis = analysis.findAll {
            it.processingState == AnalysisProcessingStates.FINISHED &&
                    !it.sampleType1BamFile.withdrawn && !it.sampleType2BamFile.withdrawn &&
                    withdrawAnalysisService.collectPaths([it]).every { path -> Files.exists(fileSystem.getPath(path)) }
        }

        withdrawStateHolder.pathsToChangeGroup.putAll(analysis.collectEntries {
            [withdrawAnalysisService.collectPaths([it]).first(), it.project.unixGroup]
        })
        if (analysis.size() > 0) {
            withdrawStateHolder.summary << "Unwithdrawing analysis result: ${analysis}"
            withdrawAnalysisService.unwithdrawObjects(analysis)
        } else {
            withdrawStateHolder.summary << "Unwithdrawing analysis result: Nothing to do"
        }
    }

    void writeBashScript(UnwithdrawStateHolder withdrawStateHolder) {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        Path outputFile = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('withdrawn').resolve(withdrawStateHolder.scriptFileName)

        fileService.deleteDirectoryRecursively(outputFile) // delete file if already exists
        fileService.createFileWithContent(outputFile, withdrawStateHolder.script.join('\n'),
                FileService.OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION)

        withdrawStateHolder.summary << "\nScript Path:"
        withdrawStateHolder.summary << outputFile
    }

    void createBashScript(UnwithdrawStateHolder withdrawStateHolder) {
        withdrawStateHolder.script << "\n#change group for links, files and directories"
        withdrawStateHolder.linksToCreate.each { target, link ->
            withdrawStateHolder.script << "mkdir -p  ${link.parent}"
            withdrawStateHolder.script << "ln -rs ${target} ${link}"
        }
        withdrawStateHolder.pathsToChangeGroup.each { path, group ->
            withdrawStateHolder.script << "chgrp --recursive --verbose ${group} ${path}"
        }

        withdrawStateHolder.script << "\necho script has run till end\n"
    }
}

class UnwithdrawStateHolder {
    List<SeqTrackWithComment> seqTracksWithComment = []

    List<String> summary = []

    Map<Path, Path> linksToCreate = [:]
    Map<String, String> pathsToChangeGroup = [:]

    List<AbstractBamFile> bamFiles = []

    List<String> script = []
    String scriptFileName

    List<SeqTrack> getSeqTracks() {
        return seqTracksWithComment*.seqTrack
    }
}
