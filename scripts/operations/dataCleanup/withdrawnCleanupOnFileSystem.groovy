/*
 * Copyright 2011-2024 The OTP authors
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
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.*

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
BamFileAnalysisServiceFactoryService bamFileAnalysisServiceFactoryService = ctx.bamFileAnalysisServiceFactoryService

String withdrawnGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP)
String chgrp = "chgrp --recursive --verbose ${withdrawnGroup}"

String bamFiles = AbstractBamFile.findAllByWithdrawn(true).findAll {
    it.isMostRecentBamFile()
}.collect {
    abstractBamFileService.getBaseDirectory(it)
}.findAll {
    Files.exists(it)
}.collectMany {
    Files.list(it) as List
}.findAll {
    !it.toString().endsWith('nonOTP')
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String analysis = BamFilePairAnalysis.findAllByWithdrawn(true).collect {
    bamFileAnalysisServiceFactoryService.getService(it).getWorkDirectory(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

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

fileService.createFileWithContent(path, script)
