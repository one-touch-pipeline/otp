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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.utils.exceptions.FileNotFoundException
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper

import java.nio.file.Path

class AbstractRoddyJobSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ClusterJob,
                DataFile,
                FileType,
                MergingCriteria,
                MergingWorkPackage,
                ProcessingOption,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                Run,
                FastqImportInstance,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SoftwareTool,
        ]
    }

    static final String STATUS_CODE_STARTED = "57427"
    static final String STATUS_CODE_FINISHED = "0"
    static final String STATUS_CODE_FAILED = "1"

    AbstractRoddyJob abstractRoddyJob
    RoddyBamFile roddyBamFile
    ClusterJobIdentifier clusterJobIdentifier
    TestConfigService testConfigService

    @TempDir
    Path tempDir

    void setup() {
        roddyBamFile = DomainFactory.createRoddyBamFile(
                roddyExecutionDirectoryNames: [],
        )
        abstractRoddyJob = Spy(AbstractRoddyJob)
        clusterJobIdentifier = new ClusterJobIdentifier(DomainFactory.createRealm(), "clusterJobId")

        testConfigService = new TestConfigService()
    }

    void cleanup() {
        testConfigService.clean()
    }

    void "test failedOrNotFinishedClusterJobs, when roddy execution directory does not exist, should fail"() {
        given:
        roddyBamFile.roddyExecutionDirectoryNames.add("exec_890420_133730004_user_analysis")
        roddyBamFile.save(flush: true)

        when:
        abstractRoddyJob.failedOrNotFinishedClusterJobs([])

        then:
        _ * abstractRoddyJob.processParameterObject >> roddyBamFile

        then:
        def e = thrown(AssertionError)
        e.message.contains("on local filesystem is not accessible or does not exist.")
    }

    void "test failedOrNotFinishedClusterJobs, when job state log file does not exist, should fail"() {
        given:
        testConfigService.addOtpProperty(OtpProperty.PATH_PROJECT_ROOT, tempDir.toString())

        String execDirName = "exec_890420_133730004_user_analysis"
        File roddyExecDir = new File(roddyBamFile.workExecutionStoreDirectory, execDirName)
        assert roddyExecDir.mkdirs()

        roddyBamFile.roddyExecutionDirectoryNames.add(execDirName)
        roddyBamFile.save(flush: true)

        when:
        abstractRoddyJob.failedOrNotFinishedClusterJobs([])

        then:
        1 * abstractRoddyJob.processParameterObject >> roddyBamFile

        then:
        def e = thrown(FileNotFoundException)
        e.message.contains("is not found in")
    }

    void "test failedOrNotFinishedClusterJobs, with correct job state log file"() {
        given:
        testConfigService.addOtpProperty(OtpProperty.PATH_PROJECT_ROOT, tempDir.toString())

        String execDirName = "exec_890420_133730004_user_analysis"
        File roddyExecDir = new File(roddyBamFile.workExecutionStoreDirectory, execDirName)
        assert roddyExecDir.mkdirs()

        CreateJobStateLogFileHelper.createJobStateLogFile(roddyExecDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_STARTED])
        ])
        roddyBamFile.roddyExecutionDirectoryNames.add(execDirName)
        roddyBamFile.save(flush: true)

        when:
        Map<ClusterJobIdentifier, String> resultMap = abstractRoddyJob.failedOrNotFinishedClusterJobs([clusterJobIdentifier])

        then:
        1 * abstractRoddyJob.processParameterObject >> roddyBamFile

        then:
        [(clusterJobIdentifier): "Status code: ${STATUS_CODE_STARTED}" as String] == resultMap
    }

    void "test failedOrNotFinishedClusterJobs, with multiple job state log files"() {
        given:
        testConfigService.addOtpProperty(OtpProperty.PATH_PROJECT_ROOT, tempDir.toString())
        Realm realm = DomainFactory.createRealm()
        ClusterJobIdentifier identifierA = new ClusterJobIdentifier(realm, "pbsId1")
        ClusterJobIdentifier identifierB = new ClusterJobIdentifier(realm, "pbsId2")
        ClusterJobIdentifier identifierC = new ClusterJobIdentifier(realm, "pbsId3")
        ClusterJobIdentifier identifierD = new ClusterJobIdentifier(realm, "pbsId4")

        // JOB A, 2 entries, statusCode = 0 => successfully finished job, no output,
        //                                   same identifier in older executionStore marked as failed, should be ignored
        // JOB B, 3 entries, statusCode != 0 => failed job
        // JOB C, 1 entry, statusCode = "57427" => still in progress
        // JOB D, 0 entries => no information found

        // jobStateLogFile for the first roddy call
        String execDirName1 = "exec_140625_102449388_SOMEUSER_WGS"
        File firstRoddyExecDir = new File(roddyBamFile.workExecutionStoreDirectory, execDirName1)
        assert firstRoddyExecDir.mkdirs()
        CreateJobStateLogFileHelper.createJobStateLogFile(firstRoddyExecDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierA.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierA.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
        ])

        // create jobStateLogFile for the second roddy call
        String execDirName2 = "exec_150625_102449388_SOMEUSER_WGS"
        File secondRoddyExecDir = new File(roddyBamFile.workExecutionStoreDirectory, execDirName2)
        assert secondRoddyExecDir.mkdirs()
        CreateJobStateLogFileHelper.createJobStateLogFile(secondRoddyExecDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierA.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierA.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierB.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierB.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierB.clusterJobId, statusCode: STATUS_CODE_FAILED, timeStamp: 100L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierC.clusterJobId, statusCode: STATUS_CODE_STARTED]),
        ])
        roddyBamFile.roddyExecutionDirectoryNames.add(execDirName1)
        roddyBamFile.roddyExecutionDirectoryNames.add(execDirName2)
        roddyBamFile.save(flush: true)

        assert new File(roddyBamFile.workExecutionStoreDirectory, "exec_890420_133730004_user_analysis").mkdirs()

        when:
        Map<ClusterJobIdentifier, String> resultMap = abstractRoddyJob.failedOrNotFinishedClusterJobs([identifierA, identifierB, identifierC, identifierD,])

        then:
        1 * abstractRoddyJob.processParameterObject >> roddyBamFile

        then:
        resultMap == [
                (identifierB): "Status code: ${STATUS_CODE_FAILED}",
                (identifierC): "Status code: ${STATUS_CODE_STARTED}",
                (identifierD): "JobStateLogFile contains no information for this cluster job.",
        ]
    }

    void "test test analyseFinishedClusterJobs, when job state log file contains no information about job ID, should return empty map"() {
        given:
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [])

        expect:
        [(clusterJobIdentifier): "JobStateLogFile contains no information for this cluster job."] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    void "test test analyseFinishedClusterJobs, when cluster job is in progress, should return empty map"() {
        given:
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_STARTED])
        ])

        expect:
        [(clusterJobIdentifier): "Status code: ${STATUS_CODE_STARTED}"] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    void "test analyseFinishedClusterJobs, when cluster job failed processing, should return specific error message"() {
        given:
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, timeStamp: 0L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, timeStamp: 10L, statusCode: STATUS_CODE_FAILED]),
        ])

        expect:
        [(clusterJobIdentifier): "Status code: ${STATUS_CODE_FAILED}"] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    void "test analyseFinishedClusterJobs, when cluster job finished successfully, should return empty map"() {
        given:
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED, timeStamp: 5L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, timeStamp: 10L]),
        ])

        expect:
        [:] == abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    void "test analyseFinishedClusterJobs, when no finished cluster job, should return empty map"() {
        given:
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry()
        ])

        expect:
        [:] == abstractRoddyJob.analyseFinishedClusterJobs([], jobStateLogFile)
    }
}
