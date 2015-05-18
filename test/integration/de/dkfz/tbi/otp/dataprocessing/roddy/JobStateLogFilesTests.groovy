package de.dkfz.tbi.otp.dataprocessing.roddy

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import org.junit.After
import org.junit.Before
import org.junit.Test

class JobStateLogFilesTests {

    ClusterJobIdentifier clusterJobIdentifier
    File testDirectory

    @Before
    void setUp() {
        clusterJobIdentifier = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId")
        testDirectory = TestCase.createEmptyTestDirectory()
    }

    @After
    void tearDown() {
        testDirectory.deleteDir()
    }

    @Test
    void testParseJobStateLogFile_WhenEntriesMatch_ShouldReturnMapWithPbsIdAndCorrespondingLogFileEntry() {
        JobStateLogFiles.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createLogFileEntry([pbsId: "pbsId", host: "host", exitCode: "exitCode", timeStamp: "0", jobClass: "jobClass"])
        JobStateLogFiles.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createLogFileEntry([pbsId: "pbsId2", host: "host", exitCode: "exitCode", timeStamp: "0", jobClass: "jobClass"])

        String content = """
${logFileEntry.toString()}
${logFileEntry2.toString()}
"""
        CreateFileHelper.createFile(new File(testDirectory.absolutePath, "testFile"), content)

        JobStateLogFiles jobStateLogFiles = JobStateLogFiles.create(testDirectory.absolutePath)

        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId).first().pbsId == logFileEntry.pbsId
        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId).first().host == logFileEntry.host
        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId).first().exitCode == logFileEntry.exitCode
        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId).first().timeStamp == logFileEntry.timeStamp
        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId).first().jobClass == logFileEntry.jobClass

        assert jobStateLogFiles.logFileEntries.get(logFileEntry2.pbsId).first().pbsId == logFileEntry2.pbsId
        assert jobStateLogFiles.logFileEntries.get(logFileEntry2.pbsId).first().host == logFileEntry2.host
        assert jobStateLogFiles.logFileEntries.get(logFileEntry2.pbsId).first().exitCode == logFileEntry2.exitCode
        assert jobStateLogFiles.logFileEntries.get(logFileEntry2.pbsId).first().timeStamp == logFileEntry2.timeStamp
        assert jobStateLogFiles.logFileEntries.get(logFileEntry2.pbsId).first().jobClass == logFileEntry2.jobClass
    }

    @Test
    void testParseJobStateLogFile_WhenEntriesDontMatch_ShouldReturnEmptyMap() {
        JobStateLogFiles.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createLogFileEntry([pbsId: "pbsId", host: "host", exitCode: "exitCode", timeStamp: "0", jobClass: "jobClass"])

        String content = """
${logFileEntry.toString().replace(":", "/")}
"""
        CreateFileHelper.createFile(new File(testDirectory.absolutePath, "testFile"), content)

        JobStateLogFiles jobStateLogFiles = JobStateLogFiles.create(testDirectory.absolutePath)

        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId)?.first()?.pbsId != logFileEntry.pbsId
        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId)?.first()?.host != logFileEntry.host
        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId)?.first()?.exitCode != logFileEntry.exitCode
        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId)?.first()?.timeStamp != logFileEntry.timeStamp
        assert jobStateLogFiles.logFileEntries.get(logFileEntry.pbsId)?.first()?.jobClass != logFileEntry.jobClass
    }

    @Test
    void testParseJobStateLogFile_WhenPathNotFound_ShouldReturnEmptyMap() {
        JobStateLogFiles jobStateLogFiles = JobStateLogFiles.create("")
        assert [:] == jobStateLogFiles.logFileEntries
    }
}
