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
package de.dkfz.tbi.otp.infrastructure

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId

class ClusterJobServiceSpec extends Specification implements DataTest, ServiceUnitTest<ClusterJobService> {

    static final LocalDate START_DATE = LocalDate.now()
    static final LocalDate END_DATE = START_DATE.plusDays(1)

    @TempDir
    Path tempDir

    FileService fileService = new FileService()
    ClusterJobService clusterJobService = new ClusterJobService()

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                JobExecutionPlan,
                ProcessingStep,
                ClusterJob,
        ]
    }

    void test_DateTimeIntervalWithHourBuckets_hourBuckets_WhenInputIs24_hours_ThenShouldReturnListOfTwentyFiveDates() {
        given:
        List dates = (0..48).collect {
            START_DATE.atStartOfDay(ZoneId.systemDefault()).plusHours(it)
        }

        expect:
        dates == new ClusterJobService.DateTimeIntervalWithHourBuckets(START_DATE, END_DATE).hourBuckets
    }

    void test_getLabels_WhenMaxEqualsThousandAndQuotEqualsTen_ShouldReturnMapWithStringListAndDoubleListEachContainingTenValuesEachValueAQuotHigherThanThePreviousValue() {
        given:
        List labels = clusterJobService.getLabels(1000, 10)

        expect:
        (1..10).collect { (it * 100.0) as String } == labels.first()
        (1..10).collect { (it * 100.0) as Double } == labels.last()
    }

    void test_getClusterJobByIdentifier() {
        given:
        ClusterJob clusterJob = DomainFactory.createClusterJob()
        ClusterJobIdentifier identifier = new ClusterJobIdentifier(clusterJob.clusterJobId)
        DomainFactory.createClusterJob(
                clusterJobId: HelperUtils.uniqueString,
        )
        DomainFactory.createClusterJob(
                clusterJobId: identifier.clusterJobId,
        )

        expect:
        clusterJobService.getClusterJobByIdentifier(identifier, clusterJob.processingStep) == clusterJob
    }

    @Unroll
    void "test getClusterJobLog when log file path is empty or it doesn't exist"() {
        given:
        ClusterJob clusterJob = Mock(ClusterJob)
        MessageSourceService messageSourceService = Mock(MessageSourceService)
        clusterJobService.messageSourceService = messageSourceService
        messageSourceService.createMessage("clusterJobService.path_to_job_log_not_set") >> "Path to job log not set"
        clusterJob.jobLog >> log

        when:
        String expectedLog = clusterJobService.getClusterJobLog(clusterJob)

        then:
        expectedLog == expected

        where:
        log                 | expected
        null                | "Path to job log not set"
        ''                  | "Path to job log not set"
    }
    void "test getClusterJobLog when log file exists"() {
        given:
        Path basePath = CreateFileHelper.createFile(tempDir.resolve("test.txt")) as Path
        ClusterJob clusterJob = Mock(ClusterJob)
        clusterJob.jobLog >> basePath.toString()
        TestFileSystemService fileSystemService = new TestFileSystemService()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        clusterJobService.fileSystemService = fileSystemService
        clusterJobService.fileService = fileService

        when:
        String log = clusterJobService.getClusterJobLog(clusterJob)

        then:
        log == basePath.text
    }

    void "test getClusterJobLog when log file doesn't exist"() {
        given:
        Path basePath = tempDir.resolve("test.txt")
        ClusterJob clusterJob = Mock(ClusterJob)
        clusterJob.jobLog >> basePath.toString()
        MessageSourceService messageSourceService = Mock(MessageSourceService)
        clusterJobService.messageSourceService = messageSourceService
        messageSourceService.createMessage("clusterJobService.non_existing_file") >> "File doesn't exist"
        TestFileSystemService fileSystemService = new TestFileSystemService()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        clusterJobService.fileSystemService = fileSystemService
        clusterJobService.fileService = fileService

        when:
        String log = clusterJobService.getClusterJobLog(clusterJob)

        then:
        log == "File doesn't exist"
    }

    @SuppressWarnings('UnnecessarySetter')
    void "test getClusterJobLog when log file is not readable"() {
        given:
        Path basePath = CreateFileHelper.createFile(tempDir.resolve("test.txt")) as Path
        basePath.toFile().setReadable(false)
        ClusterJob clusterJob = Mock(ClusterJob)
        clusterJob.jobLog >> basePath.toString()
        MessageSourceService messageSourceService = Mock(MessageSourceService)
        clusterJobService.messageSourceService = messageSourceService
        messageSourceService.createMessage("clusterJobService.unreadable_file") >> "File is not readable"
        TestFileSystemService fileSystemService = new TestFileSystemService()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        clusterJobService.fileSystemService = fileSystemService
        clusterJobService.fileService = fileService

        when:
        String log = clusterJobService.getClusterJobLog(clusterJob)

        then:
        log == "File is not readable"
    }

    void "test getClusterJobLog when we get IO exception"() {
        given:
        Path basePath = CreateFileHelper.createFile(tempDir.resolve("test.txt")) as Path
        ClusterJob clusterJob = Mock(ClusterJob)
        clusterJob.jobLog >> basePath.toString()
        TestFileSystemService fileSystemService = new TestFileSystemService()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> throw new IOException("Test IO Exception") }
        }
        clusterJobService.fileSystemService = fileSystemService
        clusterJobService.fileService = fileService

        when:
        String log = clusterJobService.getClusterJobLog(clusterJob)

        then:
        log == "Error accessing the file: Test IO Exception"
    }
}
