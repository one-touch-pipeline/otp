package migration
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
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService

import de.dkfz.tbi.otp.project.Project

import java.nio.file.*
// =============================================
// input area

/**
 * Name of the file to generate. The name must be absolute.
 */
String fileName = ''

assert fileName: 'No file name given, but this is required'
assert !fileName.contains(' '): 'File name contains spaces, which is not allowed'

ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService

FileSystem fileSystem = fileSystemService.remoteFileSystem
Path path = fileSystem.getPath(fileName)

String contentHeader = "projectId\tprojectName\torganizationalUnit\tcostCenter"
String content = Project.list().sort{
    it.name
}.collect{
    [
            it.id,
            it.name,
            it.organizationalUnit ?: '',
            it.costCenter ?: '',
    ].join('\t')
}.join('\n')

String output = [contentHeader, content].join('\n')
fileService.createFileWithContent(path, output)

println "Project data from Database has been exported to ${fileName}"
