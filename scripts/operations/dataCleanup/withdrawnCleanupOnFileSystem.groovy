/*
 * Copyright 2011-2019 The OTP authors
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
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

import java.nio.file.*

/**
 * script to change group of all withdrawn data on file system to withdrawn.
 */

//---------------------------------------
//input

/**
 * full qualified name of the script to create
 */
String file = ""

//---------------------------------------
//work

ProcessingOptionService processingOptionService = ctx.processingOptionService
LsdfFilesService lsdfFilesService = ctx.lsdfFilesService
FileService fileService = ctx.fileService
FileSystemService fileSystemService = ctx.fileSystemService
FastqcDataFilesService fastqcDataFilesService = ctx.fastqcDataFilesService
BamFileAnalysisServiceFactoryService bamFileAnalysisServiceFactoryService = ctx.bamFileAnalysisServiceFactoryService

String withdrawnGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP)
String chgrp = "chgrp --recursive --verbose ${withdrawnGroup}"

String bamFiles = AbstractMergedBamFile.findAllByWithdrawn(true).findAll {
    it.isMostRecentBamFile()
}.collect {
    it.baseDirectory
}.findAll {
    it.exists()
}.collectMany {
    it.listFiles() as List
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

String dataFiles = DataFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    lsdfFilesService.getFileFinalPathAsPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String md5sumDataFile = DataFile.findAllByFileWithdrawn(true).collect {
    lsdfFilesService.getFileMd5sumFinalPathAsPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String zipFiles = DataFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    fastqcDataFilesService.fastqcOutputPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String htmlFiles = DataFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    fastqcDataFilesService.fastqcHtmlPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String md5sumFiles = DataFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    fastqcDataFilesService.fastqcOutputMd5sumPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String dataFilesViewByPid = DataFile.findAllBySeqTrackIsNotNullAndFileWithdrawn(true).collect {
    lsdfFilesService.getFileViewByPidPathAsPath(it)
}.findAll { path ->
    path && Files.exists(path)
}.collect {
    "rm ${it}" as String
}.sort().join('\n')

String script = [
        "#/bin/bash",
        "set -ev",
        dataFiles,
        bamFiles,
        analysis,
        zipFiles,
        htmlFiles,
        md5sumFiles,
        md5sumDataFile,
        dataFilesViewByPid
].join('\n')

Path path = fileSystemService.getRemoteFileSystemOnDefaultRealm().getPath(file)

fileService.createFileWithContentOnDefaultRealm(path, script)
