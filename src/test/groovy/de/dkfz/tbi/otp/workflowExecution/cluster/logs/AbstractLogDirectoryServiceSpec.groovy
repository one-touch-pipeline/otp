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
package de.dkfz.tbi.otp.workflowExecution.cluster.logs

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.util.TimeFormats

import java.nio.file.FileSystems
import java.nio.file.Path

abstract class AbstractLogDirectoryServiceSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @TempDir
    Path tempDir

    protected TestConfigService configService

    abstract AbstractLogDirectoryService getService()

    @Override
    Class[] getDomainClassesToMock() {
        return []
    }

    void setup() {
        service.configService = configService = new TestConfigService(tempDir)
    }

    protected void mockPathDoesNotExist(Path expected, int countRemoteFileSystem = 1) {
        service.fileSystemService = Mock(FileSystemService) {
            countRemoteFileSystem * getRemoteFileSystem() >> FileSystems.default
            0 * _
        }
        service.fileService = Mock(FileService) {
            1 * createDirectoryRecursivelyAndSetPermissionsViaBash(expected)
            1 * changeFileSystem(expected, FileSystems.default) >> expected
            0 * _
        }
    }

    protected void mockPathExist(int countRemoteFileSystem = 0) {
        service.fileSystemService = Mock(FileSystemService) {
            countRemoteFileSystem * getRemoteFileSystem() >> FileSystems.default
            0 * _
        }
        service.fileService = Mock(FileService) {
            0 * _
        }
    }

    void cleanup() {
        configService.clean()
    }

    protected Path expectedPath(Date date, String directory) {
        return configService.loggingRootPath.toPath().resolve(directory).resolve(TimeFormats.DATE_DIRECTORY.getFormattedDate(date))
    }
}
