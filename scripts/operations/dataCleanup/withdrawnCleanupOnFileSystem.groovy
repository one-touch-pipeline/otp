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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Files
import java.nio.file.Path

/**
 * script to change group of all withdrawn data on file system to withdrawn.
 */

// ---------------------------------------
// input

/**
 * full qualified name of the script to create
 */
String file = ""

// ---------------------------------------
// work
AbstractBamFileService abstractBamFileService = ctx.abstractBamFileService
ProcessingOptionService processingOptionService = ctx.processingOptionService
RawSequenceDataViewFileService rawSequenceDataViewFileService = ctx.rawSequenceDataViewFileService
FileService fileService = ctx.fileService
FileSystemService fileSystemService = ctx.fileSystemService
FastqcDataFilesService fastqcDataFilesService = ctx.fastqcDataFilesService
AnalysisWorkFileServiceFactoryService analysisWorkFileServiceFactoryService = ctx.analysisWorkFileServiceFactoryService
AnalysisLinkFileServiceFactoryService analysisLinkFileServiceFactoryService = ctx.analysisLinkFileServiceFactoryService
FilestoreService filestoreService = ctx.filestoreService

String withdrawnGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP)
String unixGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
String chgrp = "chgrp --recursive --verbose ${withdrawnGroup}"

// Handle BAM files with both link paths and UUID work folder paths
List<AbstractBamFile> withdrawnBamFiles = AbstractBamFile.findAllByWithdrawn(true).findAll {
    it.isMostRecentBamFile()
}

// Collect link paths
List<String> bamFileLinkPaths = withdrawnBamFiles.collect {
    abstractBamFileService.getBaseDirectory(it)
}.findAll {
    Files.exists(it)
}.collectMany {
    Files.list(it) as List
}.findAll {
    !it.toString().endsWith('nonOTP')
}.collect {
    "${chgrp} ${it}" as String
}

// Collect UUID work folder paths for BAM files with workflowArtefact
List<String> bamFileUuidPaths = withdrawnBamFiles.findAll { bamFile ->
    bamFile.workflowArtefact?.producedBy?.workFolder
}.collect { bamFile ->
    filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
}.findAll {
    Files.exists(it)
}.collect {
    "${chgrp} ${it}" as String
}

String bamFiles = (bamFileLinkPaths + bamFileUuidPaths).sort().join('\n')

// Handle analysis with both traditional linked view paths and UUID work folder paths
List<BamFilePairAnalysis> withdrawnAnalysis = BamFilePairAnalysis.findAllByWithdrawn(true)

// Collect both linked view analysis paths and uuid paths
List<String> analysisPaths = withdrawnAnalysis.collectMany {
    [
            analysisWorkFileServiceFactoryService.getService(it).getDirectoryPath(),
            analysisLinkFileServiceFactoryService.getService(it).getDirectoryPath(),
    ]
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}

String analysis = analysisPaths.sort().join('\n')

String rawSequenceFiles = RawSequenceFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    rawSequenceDataWorkFileService.getFilePath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String md5sumRawSequenceFile = RawSequenceFile.findAllByFileWithdrawn(true).collect {
    rawSequenceDataWorkFileService.getMd5sumPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String zipFiles = RawSequenceFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(it))
}.findAll().collect {
    fastqcDataFilesService.fastqcOutputPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String htmlFiles = RawSequenceFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(it))
}.findAll().collect {
    fastqcDataFilesService.fastqcHtmlPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String md5sumFiles = RawSequenceFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(it))
}.findAll().collect {
    fastqcDataFilesService.fastqcOutputMd5sumPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String rawSequenceFilesViewByPid = RawSequenceFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    rawSequenceDataViewFileService.getFilePath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "rm ${it}" as String
}.sort().join('\n')

String script = [
        "#/bin/bash",
        "set -ev",
        rawSequenceFiles,
        bamFiles,
        analysis,
        zipFiles,
        htmlFiles,
        md5sumFiles,
        md5sumRawSequenceFile,
        rawSequenceFilesViewByPid
].join('\n')

Path path = fileSystemService.remoteFileSystem.getPath(file)

fileService.createFileWithContent(path, script, unixGroup)
