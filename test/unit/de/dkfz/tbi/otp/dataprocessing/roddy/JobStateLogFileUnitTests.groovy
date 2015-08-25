package de.dkfz.tbi.otp.dataprocessing.roddy

import de.dkfz.tbi.TestCase

import static de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile.JOB_STATE_LOG_FILE_NAME
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import static de.dkfz.tbi.TestCase.shouldFailWithMessage

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static junit.framework.Assert.assertFalse

import grails.test.mixin.Mock

@Mock([
        Realm,
])
class JobStateLogFileUnitTests {

    public static final String STATUS_CODE_FAILED = "1"
    public static final String STATUS_CODE_FINISHED = "0"

    ClusterJobIdentifier clusterJobIdentifier

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        clusterJobIdentifier = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId")
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
    void testValidateFile_WhenFileDoesNotExist_ShouldThrowException(){
        JobStateLogFile.metaClass.parseJobStateLogFile = { -> [:] }
        shouldFailWithMessage(RuntimeException, /${JOB_STATE_LOG_FILE_NAME}\sis\snot\sfound.*/) {
            JobStateLogFile.getInstance(TestCase.uniqueNonExistentPath)
        }
    }

    @Test
    void testValidateFile_WhenFileExistsButIsNotReadable_ShouldThrowException() {
        JobStateLogFile.metaClass.parseJobStateLogFile = { -> [:] }
        File file = tmpDir.newFile(JOB_STATE_LOG_FILE_NAME)
        file.createNewFile()
        file.setReadable(false)
        shouldFailWithMessage(RuntimeException, /file.*exists,\sbut\sis\snot\sreadable/) {
            JobStateLogFile.getInstance(tmpDir.root)
        }
    }

    @Test
    void testParseJobStateLogFile_WhenEntriesMatch_ShouldReturnMapWithPbsIdAndCorrespondingLogFileEntry() {
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: "pbsId1"])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: "pbsId2"])

        String content = """${logFileEntry.toString()}
${logFileEntry2.toString()}"""

        CreateFileHelper.createFile(tmpDir.newFile(JOB_STATE_LOG_FILE_NAME), content)

        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(tmpDir.root)

        assert jobStateLogFile.logFileEntries.get(logFileEntry.pbsId).first().pbsId == logFileEntry.pbsId
        assert jobStateLogFile.logFileEntries.get(logFileEntry.pbsId).first().host == logFileEntry.host
        assert jobStateLogFile.logFileEntries.get(logFileEntry.pbsId).first().statusCode == logFileEntry.statusCode
        assert jobStateLogFile.logFileEntries.get(logFileEntry.pbsId).first().timeStamp == logFileEntry.timeStamp
        assert jobStateLogFile.logFileEntries.get(logFileEntry.pbsId).first().jobClass == logFileEntry.jobClass

        assert jobStateLogFile.logFileEntries.get(logFileEntry2.pbsId).first().pbsId == logFileEntry2.pbsId
        assert jobStateLogFile.logFileEntries.get(logFileEntry2.pbsId).first().host == logFileEntry2.host
        assert jobStateLogFile.logFileEntries.get(logFileEntry2.pbsId).first().statusCode == logFileEntry2.statusCode
        assert jobStateLogFile.logFileEntries.get(logFileEntry2.pbsId).first().timeStamp == logFileEntry2.timeStamp
        assert jobStateLogFile.logFileEntries.get(logFileEntry2.pbsId).first().jobClass == logFileEntry2.jobClass
    }

    @Test
    void testParseJobStateLogFile_WhenEntriesDoNotMatch_ShouldThrowException() {
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: "pbsId1"])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: "pbsId2"])

        String modifiedLogFileEntry = logFileEntry2.toString().replace(":", "/")

        String content = """${logFileEntry.toString()}
${modifiedLogFileEntry}"""

        File file = CreateFileHelper.createFile(tmpDir.newFile(JOB_STATE_LOG_FILE_NAME), content)

        shouldFailWithMessage(RuntimeException, "${file} contains non-matching entry: ${modifiedLogFileEntry}") {
            JobStateLogFile.getInstance(tmpDir.root)
        }
    }

    @Test
    void testParseJobStateLogFile_WhenOrderOfEntriesIsWrong_ShouldThrowException() {
        JobStateLogFile.LogFileEntry logFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 20L])
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([timeStamp: 10L])

        String content = """${logFileEntry.toString()}
${logFileEntry2.toString()}"""

        CreateFileHelper.createFile(tmpDir.newFile(JOB_STATE_LOG_FILE_NAME), content)

        shouldFailWithMessage(RuntimeException, "Later JobStateLogFile entry with pbsID: testPbsId " +
                "has timestamp which is less than one for previous entries.") {
            JobStateLogFile.getInstance(tmpDir.root)
        }
    }

    @Test
    void testContainsPbsId_WhenJobStateLogFileContainsPbsId_ShouldReturnTrue() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId])
                ]
        )

        assertTrue(JobStateLogFile.containsPbsId(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testContainsPbsId_WhenJobStateLogFileDoesNotContainPbsId_ShouldReturnFalse() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId])
                ]
        )

        assertFalse(JobStateLogFile.containsPbsId("UNKNOWN"))
    }

    @Test
    void testIsClusterJobFinishedSuccessfully_WhenStatusCodeIsNull_ReturnTrue() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId])
                ]
        )

        assertTrue(JobStateLogFile.isClusterJobFinishedSuccessfully(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testIsClusterJobFinishedSuccessfully_WhenStatusCodeIsNotNull_ReturnFalse() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED])
                ]
        )

        assertFalse(JobStateLogFile.isClusterJobFinishedSuccessfully(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testGetPropertyFromLatestLogFileEntry_WhenPbsIdNotFound_ShouldReturnNull() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId])
                ]
        )

        assert null == JobStateLogFile.getPropertyFromLatestLogFileEntry("UNKNOWN", "statusCode")
    }

    @Test
    void testGetPropertyFromLatestLogFileEntry_WhenSeveralLogFileEntriesWithSamePbsId_ShouldReturnLatest() {
        JobStateLogFile JobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, statusCode: "10"]),
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L])
                ]
        )

        assert STATUS_CODE_FINISHED == JobStateLogFile.getPropertyFromLatestLogFileEntry(clusterJobIdentifier.clusterJobId, "statusCode")
    }

    @Test
    void testIsEmpty_WhenFileIsEmpty_ShouldReturnTrue() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [])

        assert jobStateLogFile.isEmpty()
    }

    @Test
    void testIsEmpty_WhenFileIsNotEmpty_ShouldReturnFalse() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(
                tmpDir.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId])
                ]
        )

        assertFalse(jobStateLogFile.isEmpty())
    }
}
