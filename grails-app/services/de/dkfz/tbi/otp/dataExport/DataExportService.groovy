/*
 * Copyright 2011-2025 The OTP authors
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
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*

@SuppressWarnings(["GStringExpressionWithinString", "DoNotUseCompileDynamicWithClasses"])
@PreAuthorize("hasRole('ROLE_OPERATOR')")
@Transactional
@CompileDynamic
class DataExportService {

    AbstractAnalysisWorkFileService abstractAnalysisWorkFileService
    FileService fileService
    FileSystemService fileSystemService
    BamFileAnalysisServiceFactoryService bamFileAnalysisServiceFactoryService
    IndividualService individualService
    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    RawSequenceDataViewFileService rawSequenceDataViewFileService

    DataExportOutput exportHeaderInfo(DataExportInput dataExportInput) {
        return exportFilesWrapper(dataExportInput, exportHeaderInfoClosure)
    }

    DataExportOutput exportRawSequenceFiles(DataExportInput dataExportInput) {
        return exportFilesWrapper(dataExportInput, exportRawSequenceFilesClosure)
    }

    DataExportOutput exportBamFiles(DataExportInput dataExportInput) {
        return exportFilesWrapper(dataExportInput, exportBamFilesClosure)
    }

    DataExportOutput exportAnalysisFiles(DataExportInput dataExportInput) {
        return exportFilesWrapper(dataExportInput, exportAnalysisFilesClosure)
    }

    private final Closure exportHeaderInfoClosure = { DataExportInput dataExportInput, StringBuilder scriptFileBuilder, StringBuilder scriptListBuilder,
                                                      StringBuilder consoleBuilder, String copyConnection, String copyTargetBase ->
        String umask = dataExportInput.external ? "027" : "022"

        if (dataExportInput.mode == DataExportInput.Mode.COPY_EXTERNAL) {
            scriptFileBuilder.append('[[ -z "${COPY_CONNECTION}" ]] && echo "COPY_CONNECTION must be set" && exit 1\n')
            scriptFileBuilder.append('[[ -z "${COPY_TARGET_BASE}" ]] && echo "COPY_TARGET_BASE must be set" && exit 1\n')
        }

        if (dataExportInput.mode != DataExportInput.Mode.LINK_INTERNAL) {
            scriptFileBuilder.append(': "${RSYNC_LOG:=--info=NAME}"\n') // RSYNC_LOG can be set to -v to get detailed logs
        }

        if (!dataExportInput.checkFileStatus) {
            scriptFileBuilder.append("#!/bin/bash\n\nset -e\numask ${umask}\n")
            scriptListBuilder.append("#!/bin/bash\n\nset -e\numask ${umask}\n")
        }

        dataExportInput.seqTrackList*.individual.unique().each { Individual individual ->
            Path targetFolderWithPid = dataExportInput.targetFolder.resolve(individual.pid)
            if (dataExportInput.checkFileStatus) {
                consoleBuilder.append("${individual.pid}\n")
            } else {
                scriptFileBuilder.append("mkdir -p ${copyTargetBase}${targetFolderWithPid}\n")
            }
        }
    }

    private final Closure exportRawSequenceFilesClosure = { DataExportInput dataExportInput, StringBuilder scriptFileBuilder, StringBuilder scriptListBuilder,
                                                            StringBuilder consoleBuilder, String copyConnection, String copyTargetBase ->
        if (dataExportInput.checkFileStatus) {
            consoleBuilder.append("\n************************************ FASTQ ************************************\n")
            if (dataExportInput.copyWithdrawnData) {
                consoleBuilder.append("Found ${dataExportInput.seqTrackList.size()} lanes:\n")
            } else {
                consoleBuilder.append("Found ${dataExportInput.seqTrackList.findAll { !it.isWithdrawn() }.size()} lanes:\n")
            }
        }

        dataExportInput.seqTrackList.each { SeqTrack seqTrack ->
            String seqTrackPid = seqTrack.individual.pid
            String seqType = seqTrack.seqType.dirName
            String sampleType = seqTrack.sampleType.dirName
            if (dataExportInput.checkFileStatus && !seqTrack.isWithdrawn()) {
                consoleBuilder.append("\n${seqTrack.individual}\t${seqTrack.seqType}\t${seqTrack.sampleType.name}\n")
            }
            seqTrack.sequenceFiles.findAll { dataExportInput.copyWithdrawnData ? true : !it.fileWithdrawn }.each { RawSequenceFile rawSequenceFile ->
                Path currentFile = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
                if (Files.exists(currentFile)) {
                    if (!dataExportInput.checkFileStatus) {
                        Path targetFastqFolder = constructTargetFolder(dataExportInput, rawSequenceFile)
                        scriptFileBuilder.append("[[ -n \"\${ECHO_LOG}\" ]] && echo ${currentFile}\n")
                        scriptFileBuilder.append("mkdir -p ${copyTargetBase}${targetFastqFolder}\n")
                        String search = "${currentFile.toString().replaceAll("(_|.)R([1,2])(_|.)", "\$1*\$2\$3")}*"
                        if (dataExportInput.mode == DataExportInput.Mode.LINK_INTERNAL) {
                            linkFilesHelper(scriptFileBuilder, search, copyTargetBase + targetFastqFolder)
                        } else {
                            scriptFileBuilder.append("rsync \${RSYNC_LOG} -upL ${copyConnection}${search} ${copyTargetBase}${targetFastqFolder}\n")
                        }

                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l ${search}\n")
                        }
                    }
                } else {
                    if (dataExportInput.checkFileStatus) {
                        consoleBuilder.append("WARNING: FastQ file ${currentFile} for ${seqTrackPid} ${sampleType} ${seqType} doesn't exist\n")
                    }
                }
            }
        }
        if (dataExportInput.unixGroup) {
            scriptFileBuilder.append("chgrp -R ${dataExportInput.unixGroup} ${copyTargetBase}${dataExportInput.targetFolder}\n")
        }
    }

    private final Closure exportBamFilesClosure = { DataExportInput dataExportInput, StringBuilder scriptFileBuilder, StringBuilder scriptListBuilder,
                                                    StringBuilder consoleBuilder, String copyConnection, String copyTargetBase ->
        if (dataExportInput.checkFileStatus) {
            consoleBuilder.append("\n************************************ BAM ************************************\n")
            consoleBuilder.append("Found BAM files ${dataExportInput.bamFileList.size()}\n")
        }

        FileSystem fileSystem = fileSystemService.remoteFileSystem

        dataExportInput.bamFileList.each { AbstractBamFile bamFile ->
            Path basePath = Paths.get(bamFile.baseDirectory.absolutePath)
            Path sourceBam = bamFile instanceof ExternallyProcessedBamFile ?
                    fileSystem.getPath(bamFile.bamFile.toString()) :
                    fileSystem.getPath(basePath.toString(), bamFile.bamFileName)
            Path qcFolder = fileSystem.getPath(basePath.toString(), "qualitycontrol")

            Path targetBamFolder = constructTargetFolder(dataExportInput, bamFile)
            Path qcTargetFolder = targetBamFolder.resolve("qualitycontrol")

            if (dataExportInput.checkFileStatus) {
                consoleBuilder.append("\n${bamFile}\n")
            }

            if (Files.exists(sourceBam)) {
                if (!dataExportInput.checkFileStatus) {
                    if ((bamFile.seqType == SeqTypeService.rnaSingleSeqType || bamFile.seqType == SeqTypeService.rnaPairedSeqType) &&
                            dataExportInput.copyAnalyses.get(PipelineType.RNA_ANALYSIS)) {
                        scriptFileBuilder.append("[[ -n \"\${ECHO_LOG}\" ]] && echo ${basePath}\n")
                        scriptFileBuilder.append("mkdir -p ${copyTargetBase}${targetBamFolder}\n")
                        if (dataExportInput.mode == DataExportInput.Mode.LINK_INTERNAL) {
                            String search = "\$(ls -d ${basePath}/* | grep -v roddyExec)"
                            linkFilesHelper(scriptFileBuilder, search, copyTargetBase + targetBamFolder)
                        } else {
                            scriptFileBuilder.append("rsync \${RSYNC_LOG} -urpL --exclude=*roddyExec* --exclude=.* ${copyConnection}${basePath} ")
                            scriptFileBuilder.append("${copyTargetBase}${targetBamFolder}\n")
                        }
                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l --ignore=\"*roddyExec*\" ${basePath}\n")
                        }
                    } else {
                        scriptFileBuilder.append("[[ -n \"\${ECHO_LOG}\" ]] && echo ${sourceBam}\n")
                        scriptFileBuilder.append("mkdir -p ${copyTargetBase}${targetBamFolder}\n")
                        if (dataExportInput.mode == DataExportInput.Mode.LINK_INTERNAL) {
                            String search = "\$(ls -d ${sourceBam}*)"
                            linkFilesHelper(scriptFileBuilder, search, copyTargetBase + targetBamFolder)
                        } else {
                            scriptFileBuilder.append("rsync \${RSYNC_LOG} -upL ${copyConnection}${sourceBam}* ${copyTargetBase}${targetBamFolder}\n")
                        }
                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l ${sourceBam}*\n")
                        }
                    }
                    if (Files.exists(qcFolder)) {
                        scriptFileBuilder.append("[[ -n \"\${ECHO_LOG}\" ]] && echo ${qcFolder}\n")
                        if (dataExportInput.mode == DataExportInput.Mode.LINK_INTERNAL) {
                            scriptFileBuilder.append("mkdir -p ${copyTargetBase}${qcTargetFolder}\n")
                            String search = "\$(ls -d ${qcFolder}/* 2>/dev/null)"
                            linkFilesHelper(scriptFileBuilder, search, copyTargetBase + qcTargetFolder)
                        } else {
                            scriptFileBuilder.append("rsync \${RSYNC_LOG} -urpL ${copyConnection}${qcFolder}* ${copyTargetBase}${qcTargetFolder}\n")
                        }
                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l ${qcFolder}*\n")
                        }
                    }
                }
            } else {
                if (dataExportInput.checkFileStatus) {
                    consoleBuilder.append("WARNING: BAM File ${sourceBam} for ${bamFile.individual.pid} ")
                    consoleBuilder.append("${bamFile.sampleType.dirName} ${bamFile.seqType.dirName} doesn't exist\n")
                }
            }
        }
        if (dataExportInput.unixGroup) {
            scriptFileBuilder.append("chgrp -R ${dataExportInput.unixGroup} ${copyTargetBase}${dataExportInput.targetFolder}\n")
        }
    }

    private final Closure exportAnalysisFilesClosure = { DataExportInput dataExportInput, StringBuilder scriptFileBuilder, StringBuilder scriptListBuilder,
                                                         StringBuilder consoleBuilder, String copyConnection, String copyTargetBase ->
        if (dataExportInput.checkFileStatus) {
            consoleBuilder.append("\n************************************ Analyses ************************************\n")
        }
        dataExportInput.analysisListMap.each { Map.Entry<PipelineType, List<BamFilePairAnalysis>> entry ->
            String pipelineName = entry.key
            List<BamFilePairAnalysis> analyses = entry.value
            if (analyses) {
                if (dataExportInput.checkFileStatus) {
                    consoleBuilder.append("\nFound following ${pipelineName} analyses:\n")
                    analyses.each {
                        consoleBuilder.append("\t${it.individual.pid}\t${it.seqType.displayName}")
                        consoleBuilder.append("\t${it.sampleType1BamFile.sampleType.name}-${it.sampleType2BamFile.sampleType.name}: ${it.instanceName}\n")
                    }
                } else {
                    analyses.each {
                        Path resultFolder = constructTargetFolder(dataExportInput, it)
                        File instancePath = fileService.toFile(bamFileAnalysisServiceFactoryService.getService(it).getWorkDirectory(it))
                        scriptFileBuilder.append("[[ -n \"\${ECHO_LOG}\" ]] && echo ${instancePath}\n")
                        scriptFileBuilder.append("mkdir -p ${copyTargetBase}${resultFolder}\n")
                        if (dataExportInput.mode == DataExportInput.Mode.LINK_INTERNAL) {
                            String search = "\$(ls -d ${instancePath}/* | grep -v roddyExec | grep -v bam)"
                            linkFilesHelper(scriptFileBuilder, search, resultFolder.toString())
                        } else {
                            scriptFileBuilder.append("rsync \${RSYNC_LOG} -urpL --exclude=*roddyExec* --exclude=*bam* ${copyConnection}${instancePath} ")
                            scriptFileBuilder.append("${copyTargetBase}${resultFolder}\n")
                        }
                        if (dataExportInput.getFileList) {
                            scriptListBuilder.append("ls -l --ignore=\"*roddyExec*\" ${instancePath}\n")
                        }
                    }
                }
            }
        }
        if (dataExportInput.unixGroup) {
            scriptFileBuilder.append("chgrp -R ${dataExportInput.unixGroup} ${copyTargetBase}${dataExportInput.targetFolder}\n")
        }
    }

    private Path constructTargetFolder(DataExportInput dataExportInput, RawSequenceFile rawSequenceFile) {
        return dataExportInput.targetFolder.resolve(rawSequenceFile.individual.pid).
                resolve(rawSequenceFile.seqTrack.seqType.dirName).
                resolve(individualService.getViewByPidPath(rawSequenceFile.individual, rawSequenceFile.seqType)
                        .relativize(rawSequenceDataViewFileService.getDirectoryPath(rawSequenceFile)))
    }

    private Path constructTargetFolder(DataExportInput dataExportInput, AbstractBamFile bamFile) {
        return dataExportInput.targetFolder.resolve(bamFile.individual.pid).
                resolve(bamFile.seqType.dirName).
                resolve(bamFile.sampleType.dirName + (bamFile.workPackage.seqType.hasAntibodyTarget ?
                        "-${bamFile.workPackage.antibodyTarget.name}" : ""))
    }

    private Path constructTargetFolder(DataExportInput dataExportInput, BamFilePairAnalysis bamFilePairAnalysis) {
        return dataExportInput.targetFolder.resolve(bamFilePairAnalysis.individual.pid).
                resolve(bamFilePairAnalysis.seqType.dirName).
                resolve("${bamFilePairAnalysis.instanceName.toLowerCase()}_results").
                resolve("${bamFilePairAnalysis.samplePair.sampleType1.dirName}_${bamFilePairAnalysis.samplePair.sampleType2.dirName}")
    }

    private DataExportOutput exportFilesWrapper(DataExportInput dataExportInput, Closure closure) {
        StringBuilder bashScriptBuilder = new StringBuilder()
        StringBuilder listScriptBuilder = new StringBuilder()
        StringBuilder consoleLogBuilder = new StringBuilder()

        String copyConnection = dataExportInput.mode == DataExportInput.Mode.COPY_EXTERNAL ? "\${COPY_CONNECTION}" : ""
        String copyTargetBase = dataExportInput.mode == DataExportInput.Mode.COPY_EXTERNAL ? "\${COPY_TARGET_BASE}" : ""

        closure(dataExportInput, bashScriptBuilder, listScriptBuilder, consoleLogBuilder, copyConnection, copyTargetBase)

        return new DataExportOutput(
                bashScript: bashScriptBuilder.toString(),
                listScript: listScriptBuilder.toString(),
                consoleLog: consoleLogBuilder.toString(),
        )
    }

    private void linkFilesHelper(StringBuilder scriptFileBuilder, String searchPattern, String targetFolder) {
        scriptFileBuilder.append("for file in ${searchPattern}; do base=\$(basename \$file) ln -sf \$file ${targetFolder}/\$base; done;\n")
    }
}

