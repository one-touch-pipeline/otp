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

import grails.test.mixin.Mock
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.FileNotFoundException
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper

import static de.dkfz.tbi.TestCase.shouldFail
import static de.dkfz.tbi.TestCase.shouldFailWithMessage
import static de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile.JOB_STATE_LOG_FILE_NAME
import static junit.framework.Assert.assertFalse
import static junit.framework.Assert.assertTrue

@Mock([
        Realm,
])
class JobStateLogFileUnitTests {

    static final String STATUS_CODE_FAILED = "1"
    static final String STATUS_CODE_FINISHED = "0"

    ClusterJobIdentifier clusterJobIdentifier

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        clusterJobIdentifier = new ClusterJobIdentifier(DomainFactory.createRealm(), "clusterJobId")
    }

    @After
    void tearDown() {
        JobStateLogFile.metaClass = null
    }

    @Test
    void testCreate_WhenInputIsCorrect_ShouldSetFileProperty() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [])
        assert new File(tmpDir.root, JOB_STATE_LOG_FILE_NAME) == jobStateLogFile.file
    }

    @Test
    void testValidateFile_WhenFileDoesNotExist_ShouldThrowException() {
        JobStateLogFile.metaClass.parseJobStateLogFile = { -> [:] }
        shouldFail(FileNotFoundException) {
            JobStateLogFile.getInstance(TestCase.uniqueNonExistentPath)
        }
    }

    @Test
    void testValidateFile_WhenFileExistsButIsNotReadable_ShouldThrowException() {
        JobStateLogFile.metaClass.parseJobStateLogFile = { -> [:] }
        File file = tmpDir.newFile(JOB_STATE_LOG_FILE_NAME)
        file.createNewFile()
        file.setReadable(false)
        shouldFail(FileNotReadableException) {
            JobStateLogFile.getInstance(tmpDir.root)
        }
    }

    @Test
    void testParseJobStateLogFile_WhenEntriesMatch_ShouldReturnMapWithClusterJobIdAndCorrespondingLogFileEntry() {
        given:
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId1"])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId2"])
        JobStateLogFile.LogFileEntry logFileEntry3 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId3"])

        String content = """\
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry)}
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry2)}
            ${CreateJobStateLogFileHelper.convertLogFileEntryToStringIncludingHost(logFileEntry3)}""".stripIndent()

        CreateFileHelper.createFile(tmpDir.newFile(JOB_STATE_LOG_FILE_NAME), content)

        when:
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(tmpDir.root)

        then:
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).clusterJobId == logFileEntry.clusterJobId
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).statusCode == logFileEntry.statusCode
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).timeStamp == logFileEntry.timeStamp
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).jobClass == logFileEntry.jobClass

        assert jobStateLogFile.logFileEntries.get(logFileEntry2.clusterJobId).clusterJobId == logFileEntry2.clusterJobId
        assert jobStateLogFile.logFileEntries.get(logFileEntry2.clusterJobId).statusCode == logFileEntry2.statusCode
        assert jobStateLogFile.logFileEntries.get(logFileEntry2.clusterJobId).timeStamp == logFileEntry2.timeStamp
        assert jobStateLogFile.logFileEntries.get(logFileEntry2.clusterJobId).jobClass == logFileEntry2.jobClass

        assert jobStateLogFile.logFileEntries.get(logFileEntry3.clusterJobId).clusterJobId == logFileEntry3.clusterJobId
        assert jobStateLogFile.logFileEntries.get(logFileEntry3.clusterJobId).statusCode == logFileEntry3.statusCode
        assert jobStateLogFile.logFileEntries.get(logFileEntry3.clusterJobId).timeStamp == logFileEntry3.timeStamp
        assert jobStateLogFile.logFileEntries.get(logFileEntry3.clusterJobId).jobClass == logFileEntry3.jobClass
    }

    @Test
    void testParseJobStateLogFile_WhenEntriesDoNotMatch_ShouldThrowException() {
        given:
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId1"])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: "jobId2"])

        String modifiedLogFileEntry = CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry2).replace(":", "/")

        String content = """\
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry)}
            ${modifiedLogFileEntry}""".stripIndent()

        when:
        File file = CreateFileHelper.createFile(tmpDir.newFile(JOB_STATE_LOG_FILE_NAME), content)

        then:
        shouldFailWithMessage(RuntimeException, "${file} contains non-matching entry: ${modifiedLogFileEntry}") {
            JobStateLogFile.getInstance(tmpDir.root)
        }
    }

    @Test
    void testParseJobStateLogFile_WhenEntriesAreUnordered_ShouldUseEntryWithTheLaterTimeStamp() {
        given:
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 20L])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 10L])

        String content = """\
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry)}
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry2)}""".stripIndent()

        CreateFileHelper.createFile(tmpDir.newFile(JOB_STATE_LOG_FILE_NAME), content)

        when:
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(tmpDir.root)

        then:
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).clusterJobId == logFileEntry.clusterJobId
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).statusCode == logFileEntry.statusCode
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).timeStamp == logFileEntry.timeStamp
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).jobClass == logFileEntry.jobClass
    }

    @Test
    void testParseJobStateLogFile_WhenEntriesAreOrdered_ShouldUseEntryWithTheLaterTimeStamp() {
        given:
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 10L])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 20L])

        String content = """\
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry)}
            ${CreateJobStateLogFileHelper.convertLogFileEntryToString(logFileEntry2)}""".stripIndent()

        CreateFileHelper.createFile(tmpDir.newFile(JOB_STATE_LOG_FILE_NAME), content)

        when:
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(tmpDir.root)

        then:
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).clusterJobId == logFileEntry2.clusterJobId
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).statusCode == logFileEntry2.statusCode
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).timeStamp == logFileEntry2.timeStamp
        assert jobStateLogFile.logFileEntries.get(logFileEntry.clusterJobId).jobClass == logFileEntry2.jobClass
    }

    @Test
    void testContainsClusterJobId_WhenJobStateLogFileContainsClusterJobId_ShouldReturnTrue() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        assertTrue(JobStateLogFile.containsClusterJobId(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testContainsClusterJobId_WhenJobStateLogFileDoesNotContainClusterJobId_ShouldReturnFalse() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        assertFalse(JobStateLogFile.containsClusterJobId("UNKNOWN"))
    }

    @Test
    void testIsClusterJobFinishedSuccessfully_WhenStatusCodeIsNull_ReturnTrue() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        assertTrue(JobStateLogFile.isClusterJobFinishedSuccessfully(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testIsClusterJobFinishedSuccessfully_WhenStatusCodeIsNotNull_ReturnFalse() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED])
        ]
        )

        assertFalse(JobStateLogFile.isClusterJobFinishedSuccessfully(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testGetPropertyFromLatestLogFileEntry_WhenClusterJobIdNotFound_ShouldReturnNull() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        assert null == JobStateLogFile.getPropertyFromLatestLogFileEntry("UNKNOWN", "statusCode")
    }

    @Test
    void testGetPropertyFromLatestLogFileEntry_WhenSeveralLogFileEntriesWithSameClusterJobId_ShouldReturnLatest() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: "10"]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
        ]
        )

        assert STATUS_CODE_FINISHED == JobStateLogFile.getPropertyFromLatestLogFileEntry(clusterJobIdentifier.clusterJobId, "statusCode")
    }

    @Test
    void testIsEmpty_WhenFileIsEmpty_ShouldReturnTrue() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [])

        assert jobStateLogFile.file.length() == 0
    }

    @Test
    void testIsEmpty_WhenFileIsNotEmpty_ShouldReturnFalse() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId])
        ]
        )

        assert jobStateLogFile.file.length() != 0
    }
}
