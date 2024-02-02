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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import spock.lang.TempDir
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory

import java.nio.file.Files
import java.nio.file.Path

class ClusterJobLoggingServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                JobDefinition,
                JobExecutionPlan,
                Process,
                ProcessingStep,
                ProcessingStepUpdate,
        ]
    }

    TestConfigService configService

    @TempDir
    Path tempDir

    ProcessingStepUpdate processingStepUpdate
    ClusterJobLoggingService service

    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_CLUSTER_LOGS_OTP): tempDir.toString()])
        processingStepUpdate = DomainFactory.createProcessingStepUpdate()
        service = new ClusterJobLoggingService()
        service.configService = configService
        service.fileService = Stub(FileService) {
            createDirectoryRecursivelyAndSetPermissionsViaBash(_) >> { Path dir ->
                Files.createDirectories(dir)
            }
        }
    }

    void cleanup() {
        configService.clean()
    }

    void "test getLogDirectory, when all fine return correct path"() {
        when:
        File logDir = service.getLogDirectory(processingStepUpdate.processingStep)

        then:
        logDir.path ==~ /${configService.loggingRootPath.path}\/${ClusterJobLoggingService.CLUSTER_LOG_BASE_DIR}\/\d{4}-\d\d-\d\d/
    }

    void "test getLogDirectory, when processingStep is null throw exception"() {
        when:
        service.getLogDirectory(null)

        then:
        AssertionError e = thrown()
        e.message.contains('processingStep')
    }

    void "test createAndGetLogDirectory, when all fine the log directory is created"() {
        given:
        service.fileSystemService = new TestFileSystemService()
        service.fileService = Mock(FileService) {
            1 * createDirectoryRecursivelyAndSetPermissionsViaBash(_) >> { Path dir ->
                Files.createDirectories(dir)
            }
        }

        when:
        File logDir = service.createAndGetLogDirectory(processingStepUpdate.processingStep)

        then:
        logDir.exists()
    }

    void "test createAndGetLogDirectory can called multiple times for same directory without error"() {
        given:
        service.fileSystemService = new TestFileSystemService()
        service.fileService = Mock(FileService) {
            3 * createDirectoryRecursivelyAndSetPermissionsViaBash(_)
        }

        when:
        service.createAndGetLogDirectory(processingStepUpdate.processingStep)
        service.createAndGetLogDirectory(processingStepUpdate.processingStep)
        service.createAndGetLogDirectory(processingStepUpdate.processingStep)

        then:
        noExceptionThrown()
    }

    void "test createAndGetLogDirectory, when processingStep is null throw exception"() {
        when:
        service.createAndGetLogDirectory(null)

        then:
        AssertionError e = thrown()
        e.message.contains('processingStep')
    }
}
