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
package de.dkfz.tbi.otp.dataExport

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*

@SuppressWarnings("GStringExpressionWithinString")
@PreAuthorize("hasRole('ROLE_OPERATOR')")
@Transactional
class DataExportService {

    LsdfFilesService lsdfFilesService
    FileService fileService
    FileSystemService fileSystemService
    BamFileAnalysisServiceFactoryService bamFileAnalysisServiceFactoryService

    DataExportOutput exportHeaderInfo(DataExportInput dataExportInput) {
        exportFilesWrapper(dataExportInput, exportHeaderInfoClosure,)
    }

    DataExportOutput exportDataFiles(DataExportInput dataExportInput) {
        exportFilesWrapper(dataExportInput, exportDataFilesClosure)
    }

    DataExportOutput exportBamFiles(DataExportInput dataExportInput) {
        exportFilesWrapper(dataExportInput, exportBamFilesClosure)
    }

    DataExportOutput exportAnalysisFiles(DataExportInput dataExportInput) {
        exportFilesWrapper(dataExportInput, exportAnalysisFilesClosure)
    }

    private Closure exportHeaderInfoClosure = { dataExportInput, scriptFileBuilder, scriptListBuilder, consoleBuilder,
                                                rsyncChmod, copyConnection, copyTargetBase ->
        String umask = dataExportInput.external ? "027" : "022"

        if (!dataExportInput.checkFileStatus) {
            scriptFileBuilder.append("#!/bin/bash\n\nset -e\numask ${umask}\n")
            scriptListBuilder.append("#!/bin/bash\n\nset -e\numask ${umask}\n")
        }

        dataExportInput.seqTrackList*.individual.unique().each { Individual individual ->
            Path targetFolderWithPid = dataExportInput.targetFolder.resolve(individual.mockPid)
            if (dataExportInput.checkFileStatus) {
                consoleBuilder.append("${individual.mockPid}\n")
            } else {
                scriptFileBuilder.append("mkdir -p ${copyTargetBase}${targetFolderWithPid}\n")
            }
        }
    }

    private Closure exportDataFilesClosure = { dataExportInput, scriptFileBuilder, scriptListBuilder, consoleBuilder,
                                               rsyncChmod, copyConnection, copyTargetBase ->
        if (dataExportInput.checkFileStatus) {
            consoleBuilder.append("\n************************************ FASTQ ************************************\n")
            consoleBuilder.append("Found ${dataExportInput.seqTrackList.size()} lanes:\n")
        }

        FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm

        dataExportInput.seqTrackList.each { SeqTrack seqTrack ->
            String seqTrackPid = seqTrack.individual.pid
            String seqType = seqTrack.seqType.dirName
            String sampleType = seqTrack.sampleType.dirName
            if (dataExportInput.checkFileStatus) {
                consoleBuilder.append("\n${seqTrack.individual}\t${seqTrack.seqType}\t${seqTrack.sampleType.name}\n")
            }
            seqTrack.dataFiles.findAll { !it.fileWithdrawn }.each { DataFile dataFile ->
                Path currentFile = fileSystem.getPath(lsdfFilesService.getFileFinalPath(dataFile))
                if (Files.exists(currentFile)) {
                    if (!dataExportInput.checkFileStatus) {
                        Path targetFolderWithPid = dataExportInput.targetFolder.resolve(dataFile.individual.mockPid)
                        Path targetFastqFolder = targetFolderWithPid.resolve(seqTrack.seqType.dirName).
                                resolve(lsdfFilesService.getFilePathInViewByPid(dataFile)).parent
                        scriptFileBuilder.append("echo ${currentFile}\n")
                        scriptFileBuilder.append("mkdir -p ${copyTargetBase}${targetFastqFolder}\n")
                        String search = "${currentFile.toString().replaceAll("(_|.)R([1,2])(_|.)", "\$1*\$2\$3")}*"
                        scriptFileBuilder.append("rsync -uvpL --chmod=${rsyncChmod} ${copyConnection}${search} ${copyTargetBase}${targetFastqFolder}\n")

                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l ${search}\n")
                        }

                        scriptFileBuilder.append("chgrp -R ${dataExportInput.unixGroup} ${targetFolderWithPid}\n")
                    }
                } else {
                    if (dataExportInput.checkFileStatus) {
                        consoleBuilder.append("WARNING: FastQ file ${currentFile} for ${seqTrackPid} ${sampleType} ${seqType} doesn't exist\n")
                    }
                }
            }
        }
    }

    private Closure exportBamFilesClosure = { dataExportInput, scriptFileBuilder, scriptListBuilder, consoleBuilder,
                                              rsyncChmod, copyConnection, copyTargetBase ->
        if (dataExportInput.checkFileStatus) {
            consoleBuilder.append("\n************************************ BAM ************************************\n")
            consoleBuilder.append("Found BAM files ${dataExportInput.bamFileList.size()}\n")
        }

        FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm

        dataExportInput.bamFileList.each { AbstractMergedBamFile bamFile ->
            Path basePath = Paths.get(bamFile.baseDirectory.absolutePath)
            Path sourceBam = bamFile instanceof ExternallyProcessedMergedBamFile ?
                    fileSystem.getPath(bamFile.bamFile.toString()) :
                    fileSystem.getPath(basePath.toString(), bamFile.bamFileName)
            Path qcFolder = fileSystem.getPath(basePath.toString(), "qualitycontrol")

            Path targetBamFolder = dataExportInput.targetFolder.
                    resolve(bamFile.individual.mockPid).
                    resolve(bamFile.seqType.dirName).
                    resolve(bamFile.sampleType.dirName + (bamFile.workPackage.seqType.hasAntibodyTarget ?
                            "-${bamFile.workPackage.antibodyTarget.name}" : ""))

            if (dataExportInput.checkFileStatus) {
                consoleBuilder.append("\n${bamFile}\n")
            }

            if (Files.exists(sourceBam)) {
                if (!dataExportInput.checkFileStatus) {
                    if ((bamFile.seqType == SeqTypeService.rnaSingleSeqType || bamFile.seqType == SeqTypeService.rnaPairedSeqType) &&
                            dataExportInput.copyAnalyses.get(PipelineType.RNA_ANALYSIS)) {
                        scriptFileBuilder.append("echo ${basePath}\n")
                        scriptFileBuilder.append("mkdir -p ${copyTargetBase}${targetBamFolder}\n")
                        scriptFileBuilder.append("rsync -uvrpL --exclude=*roddyExec* --exclude=.* --chmod=${rsyncChmod} ${copyConnection}${basePath} ")
                        scriptFileBuilder.append("${copyTargetBase}${targetBamFolder}\n")
                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l --ignore=\"*roddyExec*\" ${basePath}\n")
                        }
                    } else {
                        scriptFileBuilder.append("echo ${sourceBam}\n")
                        scriptFileBuilder.append("mkdir -p ${copyTargetBase}${targetBamFolder}\n")
                        scriptFileBuilder.append("rsync -uvpL --chmod=${rsyncChmod} ${copyConnection}${sourceBam}* ${copyTargetBase}${targetBamFolder}\n")
                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l ${sourceBam}*\n")
                        }
                    }
                    if (Files.exists(qcFolder)) {
                        scriptFileBuilder.append("echo ${qcFolder}\n")
                        scriptFileBuilder.append("rsync -uvrpL --chmod=${rsyncChmod} ${copyConnection}${qcFolder}* ${copyTargetBase}${targetBamFolder}\n")
                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l ${qcFolder}*\n")
                        }
                    }

                    Path targetFolderWithPid = dataExportInput.targetFolder.resolve(bamFile.sample.individual.mockPid)
                    scriptFileBuilder.append("chgrp -R ${dataExportInput.unixGroup} ${targetFolderWithPid}\n")
                }
            } else {
                if (dataExportInput.checkFileStatus) {
                    consoleBuilder.append("WARNING: BAM File ${sourceBam} for ${bamFile.individual.pid} ")
                    consoleBuilder.append("${bamFile.sampleType.dirName} ${bamFile.seqType.dirName} doesn't exist\n")
                }
            }
        }
    }

    private Closure exportAnalysisFilesClosure = { dataExportInput, scriptFileBuilder, scriptListBuilder, consoleBuilder,
                                                   rsyncChmod, copyConnection, copyTargetBase ->
        if (dataExportInput.checkFileStatus) {
            consoleBuilder.append("\n************************************ Analyses ************************************\n")
        }
        dataExportInput.analysisListMap.each { Map.Entry<String, List<BamFilePairAnalysis>> entry ->
            String instanceName = entry.key
            List<BamFilePairAnalysis> analyses = entry.value
            if (analyses) {
                if (dataExportInput.checkFileStatus) {
                    consoleBuilder.append("\nFound following ${instanceName} analyses:\n")
                    analyses.each {
                        consoleBuilder.append("\t${it.individual.mockPid}\t${it.seqType.displayName}")
                        consoleBuilder.append("\t${it.sampleType1BamFile.sampleType.name}-${it.sampleType2BamFile.sampleType.name}:  " +
                                "${it.instanceName}\n")
                    }
                } else {
                    analyses.each {
                        Path targetFolderWithPid = dataExportInput.targetFolder.resolve(it.individual.mockPid)
                        Path resultFolder = targetFolderWithPid.resolve(it.seqType.dirName).
                                resolve("${instanceName.toLowerCase()}_results").
                                resolve("${it.samplePair.sampleType1.dirName}_${it.samplePair.sampleType2.dirName}")

                        File instancePath = fileService.toFile(bamFileAnalysisServiceFactoryService.getService(it).getWorkDirectory(it))
                        scriptFileBuilder.append("echo ${instancePath}\n")
                        scriptFileBuilder.append("mkdir -p ${copyTargetBase}${resultFolder}\n")
                        scriptFileBuilder.append("rsync -uvrpL --exclude=*roddyExec* --exclude=*bam* --chmod=${rsyncChmod} ${copyConnection}${instancePath} ")
                        scriptFileBuilder.append("${copyTargetBase}${resultFolder}\n")
                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l --ignore=\"*roddyExec*\" ${instancePath}\n")
                        }

                        if (!dataExportInput.checkFileStatus && !scriptListBuilder.contains(targetFolderWithPid.toString())) {
                            scriptFileBuilder.append("chgrp -R ${dataExportInput.unixGroup} ${targetFolderWithPid}\n")
                        }
                    }
                }
            }
        }
    }

    private DataExportOutput exportFilesWrapper(DataExportInput dataExportInput, Closure closure) {
        StringBuilder bashScriptBuilder = new StringBuilder()
        StringBuilder listScriptBuilder = new StringBuilder()
        StringBuilder consoleLogBuilder = new StringBuilder()
        String rsyncChmod = dataExportInput.external ? "Du=rwx,Dg=rx,Fu=rw,Fg=r" : "Du=rwx,Dgo=rx,Fu=rw,Fog=r"
        String copyConnection = dataExportInput.copyExternal ? "\${COPY_CONNECTION}" : ""
        String copyTargetBase = dataExportInput.copyExternal ? "\${COPY_TARGET_BASE}" : ""

        closure(dataExportInput, bashScriptBuilder, listScriptBuilder, consoleLogBuilder, rsyncChmod, copyConnection, copyTargetBase)

        return new DataExportOutput(
                bashScript: bashScriptBuilder.toString(),
                listScript: listScriptBuilder.toString(),
                consoleLog: consoleLogBuilder.toString(),
        )
    }
}
