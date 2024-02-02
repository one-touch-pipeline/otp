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
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class JobStatusLoggingServiceFailedOrNotFinishedClusterJobsSpec extends Specification implements DataTest, ServiceUnitTest<JobStatusLoggingService> {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingStep,
        ]
    }

    TestConfigService configService

    final static Long ARBITRARY_ID = 23
    final static Long ARBITRARY_PROCESS_ID = 12345

    File tempDirectory
    ProcessingStep processingStep
    ClusterJobIdentifier successfulJob1
    ClusterJobIdentifier failedJob1
    Collection<String> jobIds1
    ClusterJobIdentifier successfulJob2
    ClusterJobIdentifier failedJob12
    ClusterJobIdentifier failedJob22
    ClusterJobIdentifier mixedUpJob1
    ClusterJobIdentifier mixedUpJob2
    Collection<String> jobIds2
    Collection<ClusterJobIdentifier> unsuccessfulJobs2
    ClusterJobIdentifier jobWithEmptyLogFile
    ClusterJobIdentifier jobWithoutLogFile
    ClusterJobIdentifier jobWithoutLogDir
    ClusterJobIdentifier successfulJobWithSameLogDirAs1
    ClusterJobIdentifier failedJobWithSameLogDirAs1
    Collection<String> jobIdsWithSameLogDirAs1
    Collection<ClusterJobIdentifier> allUnsuccessfulJobs

    void setup() {
        tempDirectory = TestCase.createEmptyTestDirectory()

        configService = new TestConfigService([(OtpProperty.PATH_CLUSTER_LOGS_OTP): tempDirectory.path])
        service.configService = configService

        processingStep = DomainFactory.createProcessingStep([
                id           : ARBITRARY_ID,
                process      : DomainFactory.createProcess([id: ARBITRARY_PROCESS_ID]),
                jobClass     : 'this.is.a.DummyJob',
        ])

        assert new File(service.logFileBaseDir(processingStep)).mkdirs()

        final String id1 = '1001'
        final String id2 = '1002'
        /* prefixOfId3 and suffixOfId3 are for testing that the code works correctly even if the
         * constructed log message of a failed job (failedJob*2) is a prefix/suffix of the log message of a successful
         * job (successfulJob2).
         */
        final String prefixOfId3 = '12'
        final String suffixOfId3 = '34'
        final String id3 = prefixOfId3 + suffixOfId3
        successfulJob1 = new ClusterJobIdentifier(id1)
        failedJob1 = new ClusterJobIdentifier(id2)
        jobIds1 = [
                successfulJob1.clusterJobId,
                failedJob1.clusterJobId,
        ]
        successfulJob2 = new ClusterJobIdentifier(id3)
        failedJob12 = new ClusterJobIdentifier(prefixOfId3)
        failedJob22 = new ClusterJobIdentifier(suffixOfId3)
        mixedUpJob1 = new ClusterJobIdentifier("4")
        mixedUpJob2 = new ClusterJobIdentifier("5")
        jobIds2 = [
                successfulJob2.clusterJobId,
                failedJob12.clusterJobId,
                failedJob22.clusterJobId,
                mixedUpJob1.clusterJobId,
                mixedUpJob2.clusterJobId,
        ]
        unsuccessfulJobs2 = [
                failedJob12,
                failedJob22,
                mixedUpJob1,
                mixedUpJob2,
        ]
        jobWithEmptyLogFile = new ClusterJobIdentifier("6")
        jobWithoutLogFile = new ClusterJobIdentifier("7")
        jobWithoutLogDir = new ClusterJobIdentifier("8")
        successfulJobWithSameLogDirAs1 = new ClusterJobIdentifier("9")
        failedJobWithSameLogDirAs1 = new ClusterJobIdentifier("10")
        jobIdsWithSameLogDirAs1 = [
                successfulJobWithSameLogDirAs1.clusterJobId,
                failedJobWithSameLogDirAs1.clusterJobId,
        ]
        allUnsuccessfulJobs = [
                failedJob1,
                failedJob12,
                failedJob22,
                mixedUpJob1,
                mixedUpJob2,
                jobWithEmptyLogFile,
                jobWithoutLogFile,
                jobWithoutLogDir,
                failedJobWithSameLogDirAs1,
        ]

        new File(service.constructLogFileLocation(processingStep, successfulJob1.clusterJobId)).text =
                service.constructMessage(processingStep, successfulJob1.clusterJobId) + "\n"
        new File(service.constructLogFileLocation(processingStep, successfulJob2.clusterJobId)).text =
                "\n" + service.constructMessage(processingStep, successfulJob2.clusterJobId)
        new File(service.constructLogFileLocation(processingStep, jobWithEmptyLogFile.clusterJobId)).text = ''
        new File(service.constructLogFileLocation(processingStep, successfulJobWithSameLogDirAs1.clusterJobId)).text =
                service.constructMessage(processingStep, successfulJobWithSameLogDirAs1.clusterJobId)
    }

    void cleanup() {
        assert tempDirectory.deleteDir()
        configService.clean()
    }

    void "test failedOrNotFinishedClusterJobs, with collection of IDs"() {
        expect:
        service.failedOrNotFinishedClusterJobs2(processingStep, jobIds1.collect {
            new ClusterJobIdentifier(it)
        }) == [failedJob1]
    }

    void "test failedOrNotFinishedClusterJobs, with collection of different IDs"() {
        when:
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs2(processingStep, jobIds2.collect {
            new ClusterJobIdentifier(it)
        })

        then:
        unsuccessfulClusterJobs.size() == unsuccessfulJobs2.size()
        unsuccessfulClusterJobs.containsAll(unsuccessfulJobs2)
    }

    void "test failedOrNotFinishedClusterJobs, with emptyLogFile and collection of IDs"() {
        expect:
        service.failedOrNotFinishedClusterJobs2(processingStep, [jobWithEmptyLogFile]) == [jobWithEmptyLogFile]
    }

    void "test failedOrNotFinishedClusterJobs, with sameLogDirAs1 and collection of IDs"() {
        expect:
        service.failedOrNotFinishedClusterJobs2(processingStep, jobIdsWithSameLogDirAs1.collect {
            new ClusterJobIdentifier(it)
        }) == [failedJobWithSameLogDirAs1]
    }

    void "test failedOrNotFinishedClusterJobs, with collection of cluster job identifiers"() {
        when:
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs(processingStep, [
                successfulJob1,
                failedJob1,
                successfulJob2,
                failedJob12,
                failedJob22,
                mixedUpJob1,
                mixedUpJob2,
                jobWithEmptyLogFile,
                jobWithoutLogFile,
                jobWithoutLogDir,
                successfulJobWithSameLogDirAs1,
                failedJobWithSameLogDirAs1,
        ])

        then:
        unsuccessfulClusterJobs.size() == allUnsuccessfulJobs.size()
        unsuccessfulClusterJobs.containsAll(allUnsuccessfulJobs)
    }

    void "test failedOrNotFinishedClusterJobs, with argument map"() {
        when:
        final Collection<ClusterJobIdentifier> unsuccessfulClusterJobs = service.failedOrNotFinishedClusterJobs2(processingStep, [
                successfulJob1,
                failedJob1,
                successfulJob2,
                failedJob12,
                failedJob22,
                mixedUpJob1,
                mixedUpJob2,
                jobWithEmptyLogFile,
                jobWithoutLogFile,
                jobWithoutLogDir,
                successfulJobWithSameLogDirAs1,
                failedJobWithSameLogDirAs1,
        ])

        then:
        unsuccessfulClusterJobs.size() == allUnsuccessfulJobs.size()
        unsuccessfulClusterJobs.containsAll(allUnsuccessfulJobs)
    }
}
