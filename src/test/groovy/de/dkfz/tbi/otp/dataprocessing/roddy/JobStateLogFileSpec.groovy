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
package de.dkfz.tbi.otp.dataprocessing.roddy

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.exceptions.FileNotFoundException
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper

import java.nio.file.Path

import static de.dkfz.tbi.TestCase.shouldFailWithMessage
import static de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile.JOB_STATE_LOG_FILE_NAME

class JobStateLogFileSpec extends Specification implements DataTest {

    static final String STATUS_CODE_FAILED = "1"
    static final String STATUS_CODE_FINISHED = "0"

    ClusterJobIdentifier clusterJobIdentifier

    @TempDir
    Path tempDir

    @Override
    Class<?>[] getDomainClassesToMock() {
        return []
    }

    void setup() {
        clusterJobIdentifier = new ClusterJobIdentifier("clusterJobId")
    }

    void testCreate_WhenInputIsCorrect_ShouldSetFileProperty() {
        given:
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [])

        expect:
        new File(tempDir.toFile(), JOB_STATE_LOG_FILE_NAME) == jobStateLogFile.file
    }

    void testValidateFile_WhenFileDoesNotExist_ShouldThrowException() {
        given:
        JobStateLogFile.metaClass.parseJobStateLogFile = { -> [:] }

        when:
        JobStateLogFile.getInstance(TestCase.uniqueNonExistentPath)

        then:
        thrown FileNotFoundException
    }

    void testValidateFile_WhenFileExistsButIsNotReadable_ShouldThrowException() {
        given:
        JobStateLogFile.metaClass.parseJobStateLogFile = { -> [:] }
        File file = tempDir.resolve(JOB_STATE_LOG_FILE_NAME).toFile()
        file.createNewFile()
        file.readable = false

        when:
        JobStateLogFile.getInstance(tempDir.toFile())

        then:
        thrown FileNotReadableException
    }

    void testParseJobStateLogFile_WhenEntriesMatch_ShouldReturnMapWithClusterJobIdAndCorrespondingLogFileEntry() {
        given:
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId1"])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId2"])
        JobStateLogFile.LogFileEntry logFileEntry3 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId3"])

        String content = """\
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry)}
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry2)}
            ${CreateJobStateLogFileHelper.convertLogFileEntryToStringIncludingHost(logFileEntry3)}""".stripIndent()

        CreateFileHelper.createFile(tempDir.resolve(JOB_STATE_LOG_FILE_NAME), content)

        when:
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(tempDir.toFile())

        then:
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).clusterJobId == logFileEntry.clusterJobId
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).statusCode == logFileEntry.statusCode
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).timeStamp == logFileEntry.timeStamp
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).jobClass == logFileEntry.jobClass

        jobStateLogFile.logFileEntries.get(logFileEntry2.clusterJobId).clusterJobId == logFileEntry2.clusterJobId
        jobStateLogFile.logFileEntries.get(logFileEntry2.clusterJobId).statusCode == logFileEntry2.statusCode
        jobStateLogFile.logFileEntries.get(logFileEntry2.clusterJobId).timeStamp == logFileEntry2.timeStamp
        jobStateLogFile.logFileEntries.get(logFileEntry2.clusterJobId).jobClass == logFileEntry2.jobClass

        jobStateLogFile.logFileEntries.get(logFileEntry3.clusterJobId).clusterJobId == logFileEntry3.clusterJobId
        jobStateLogFile.logFileEntries.get(logFileEntry3.clusterJobId).statusCode == logFileEntry3.statusCode
        jobStateLogFile.logFileEntries.get(logFileEntry3.clusterJobId).timeStamp == logFileEntry3.timeStamp
        jobStateLogFile.logFileEntries.get(logFileEntry3.clusterJobId).jobClass == logFileEntry3.jobClass
    }

    void testParseJobStateLogFile_WhenEntriesDoNotMatch_ShouldThrowException() {
        given:
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId1"])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId2"])

        String modifiedLogFileEntry = CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry2).replace(":", "/")

        String content = """\
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry)}
            ${modifiedLogFileEntry}""".stripIndent()

        when:
        File file = CreateFileHelper.createFile(tempDir.resolve(JOB_STATE_LOG_FILE_NAME), content).toFile()

        then:
        shouldFailWithMessage(RuntimeException, "${file} contains non-matching entry: ${modifiedLogFileEntry}") {
            JobStateLogFile.getInstance(tempDir.toFile())
        }
    }

    void testParseJobStateLogFile_WhenEntriesAreUnordered_ShouldUseEntryWithTheLaterTimeStamp() {
        given:
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 20L])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 10L])

        String content = """\
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry)}
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry2)}""".stripIndent()

        CreateFileHelper.createFile(tempDir.resolve(JOB_STATE_LOG_FILE_NAME), content)

        when:
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(tempDir.toFile())

        then:
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).clusterJobId == logFileEntry.clusterJobId
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).statusCode == logFileEntry.statusCode
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).timeStamp == logFileEntry.timeStamp
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).jobClass == logFileEntry.jobClass
    }

    void testParseJobStateLogFile_WhenEntriesAreOrdered_ShouldUseEntryWithTheLaterTimeStamp() {
        given:
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 10L])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 20L])

        String content = """\
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry)}
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry2)}""".stripIndent()

        CreateFileHelper.createFile(tempDir.resolve(JOB_STATE_LOG_FILE_NAME), content)

        when:
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(tempDir.toFile())

        then:
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).clusterJobId == logFileEntry2.clusterJobId
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).statusCode == logFileEntry2.statusCode
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).timeStamp == logFileEntry2.timeStamp
        jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).jobClass == logFileEntry2.jobClass
    }

    void testContainsClusterJobId_WhenJobStateLogFileContainsClusterJobId_ShouldReturnTrue() {
        given:
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        expect:
        JobStateLogFile.containsClusterJobId(clusterJobIdentifier.clusterJobId) == true
    }

    void testContainsClusterJobId_WhenJobStateLogFileDoesNotContainClusterJobId_ShouldReturnFalse() {
        given:
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        expect:
        JobStateLogFile.containsClusterJobId("UNKNOWN") == false
    }

    void testIsClusterJobFinishedSuccessfully_WhenStatusCodeIsNull_ReturnTrue() {
        given:
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        expect:
        JobStateLogFile.isClusterJobFinishedSuccessfully(clusterJobIdentifier.clusterJobId) == true
    }

    void testIsClusterJobFinishedSuccessfully_WhenStatusCodeIsNotNull_ReturnFalse() {
        given:
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED])
        ]
        )

        expect:
        JobStateLogFile.isClusterJobFinishedSuccessfully(clusterJobIdentifier.clusterJobId) == false
    }

    void testGetPropertyFromLatestLogFileEntry_WhenClusterJobIdNotFound_ShouldReturnNull() {
        given:
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        expect:
        JobStateLogFile.getPropertyFromLatestLogFileEntry("UNKNOWN", "statusCode") == null
    }

    void testGetPropertyFromLatestLogFileEntry_WhenSeveralLogFileEntriesWithSameClusterJobId_ShouldReturnLatest() {
        given:
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: "10"]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
        ]
        )

        expect:
        STATUS_CODE_FINISHED == JobStateLogFile.getPropertyFromLatestLogFileEntry(clusterJobIdentifier.clusterJobId, "statusCode")
    }

    void testIsEmpty_WhenFileIsEmpty_ShouldReturnTrue() {
        given:
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [])

        expect:
        jobStateLogFile.file.length() == 0
    }

    void testIsEmpty_WhenFileIsNotEmpty_ShouldReturnFalse() {
        given:
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tempDir.toFile(), [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        expect:
        jobStateLogFile.file.length() != 0
    }
}
