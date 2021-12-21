/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.cluster.logs

import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.*

@CompileStatic
@Transactional
abstract class AbstractLogDirectoryService {

    ConfigService configService

    FileService fileService

    FileSystemService fileSystemService

    protected Path createAndGetLogDirectoryHelper(Date date, String logType) {
        assert date

        Path baseLogDirectory = fileSystemService.remoteFileSystemOnDefaultRealm.getPath(configService.loggingRootPath.absolutePath)
        String dateDirectory = TimeFormats.DATE_DIRECTORY.getFormattedDate(date)

        Path logPath = baseLogDirectory.resolve(logType).resolve(dateDirectory)
        if (!Files.exists(logPath)) {
            FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm
            Path remoteLogPath = fileService.changeFileSystem(logPath, fileSystem)
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(remoteLogPath, configService.defaultRealm)
        }
        return logPath
    }

    protected abstract Path createAndGetLogDirectory(Date date)
}
