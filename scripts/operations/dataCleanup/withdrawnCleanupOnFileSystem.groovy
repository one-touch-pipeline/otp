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

import java.nio.file.Files
import java.nio.file.Path

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
BamFileAnalysisServiceFactoryService bamFileAnalysisServiceFactoryService = ctx.bamFileAnalysisServiceFactoryService

String withdrawnGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP)
String chgrp = "chgrp --recursive --verbose ${withdrawnGroup}"

String bamFiles =  AbstractMergedBamFile.findAllByWithdrawn(true).findAll {
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
}.findAll {
    Files.exists(it)
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String dataFiles = DataFile.findAllByFileWithdrawn(true).collect {
    lsdfFilesService.getFileFinalPath(it)
}.findAll {
    it && new File(it).exists()
}.collect {
    "${chgrp} ${it}" as String
}.sort().join('\n')

String script = [
        "#/bin/bash",
        "set -ev",
        dataFiles,
        bamFiles,
        analysis,
].join('\n')

Path path = fileSystemService.getRemoteFileSystemOnDefaultRealm().getPath(file)

fileService.createFileWithContentOnDefaultRealm(path, script)
