package de.dkfz.tbi.otp.dataprocessing.roddy

import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import org.junit.Before
import org.junit.Test

class JobStateLogFilesUnitTests {

    public static final String TEST_HOST = "testHost"
    public static final String TEST_EXIT_CODE = "testExitCode"
    public static final String TEST_JOB_CLASS = "testJobClass"

    ClusterJobIdentifier clusterJobIdentifier

    @Before
    void setUp() {
        clusterJobIdentifier = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId")
    }

    @Test
    void testContainsPbsId_WhenJobStateLogFileContainsPbsId_ShouldReturnTrue() {
        JobStateLogFiles jobStateLogFiles = CreateJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: TEST_EXIT_CODE,
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assertTrue(jobStateLogFiles.containsPbsId(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testContainsPbsId_WhenJobStateLogFileDoesNotContainPbsId_ShouldReturnFalse() {
        JobStateLogFiles jobStateLogFiles = CreateJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: TEST_EXIT_CODE,
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assertFalse(jobStateLogFiles.containsPbsId("UNKNOWN"))
    }

    @Test
    void testIsClusterJobInProgress_WhenLessThanTwoEntriesForPbsId_ShouldReturnTrue() {
        JobStateLogFiles jobStateLogFiles = CreateJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: TEST_EXIT_CODE,
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assertTrue(jobStateLogFiles.isClusterJobInProgress(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testIsClusterJobInProgress_WhenTwoOrMoreEntriesForPbsId_ShouldReturnFalse() {
        JobStateLogFiles jobStateLogFiles = CreateJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: TEST_EXIT_CODE,
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ],
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: TEST_EXIT_CODE,
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assertFalse(jobStateLogFiles.isClusterJobInProgress(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testIsClusterJobFinishedSuccessfully_WhenExitCodeIsNull_ReturnTrue() {
        JobStateLogFiles jobStateLogFiles = CreateJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: "0",
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assertTrue(jobStateLogFiles.isClusterJobFinishedSuccessfully(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testIsClusterJobFinishedSuccessfully_WhenExitCodeIsNotNull_ReturnFalse() {
        JobStateLogFiles jobStateLogFiles = CreateJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: "1",
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assertFalse(jobStateLogFiles.isClusterJobFinishedSuccessfully(clusterJobIdentifier.clusterJobId))
    }

    @Test
    void testGetPropertyFromLatestLogFileEntry_WhenPbsIdNotFound_ShouldReturnNull() {
        JobStateLogFiles jobStateLogFiles = CreateJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: "0",
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assert null == jobStateLogFiles.getPropertyFromLatestLogFileEntry("UNKNOWN", "exitCode")
    }

    @Test
    void testGetPropertyFromLatestLogFileEntry_WhenSeveralLogFileEntriesWithSamePbsId_ShouldReturnLatest() {
        JobStateLogFiles jobStateLogFiles = CreateJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: "10",
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ],
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        node: TEST_HOST,
                        exitCode: "0",
                        timeStamp: "10",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assert "0" == jobStateLogFiles.getPropertyFromLatestLogFileEntry(clusterJobIdentifier.clusterJobId, "exitCode")
    }
}
