package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import grails.buildtestdata.mixin.Build
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static de.dkfz.tbi.TestCase.shouldFail
import static de.dkfz.tbi.TestCase.shouldFailWithMessage

@Build([RoddyBamFile, SeqPlatform])
class AbstractRoddyJobUnitTests {

    public static final String STATUS_CODE_STARTED = "57427"
    public static final String STATUS_CODE_FINISHED = "0"
    public static final String STATUS_CODE_FAILED = "1"

    AbstractRoddyJob abstractRoddyJob
    RoddyBamFile roddyBamFile
    ClusterJobIdentifier clusterJobIdentifier

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        abstractRoddyJob = [processingStepId: 123456789, getProcessParameterObject: { roddyBamFile }] as AbstractRoddyJob
        clusterJobIdentifier = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId")
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenRoddyExecutionDirectoryDoesNotExist_ShouldFail() {
        roddyBamFile.metaClass.getTmpRoddyExecutionStoreDirectory = { ->
            return new File("/does/not/exist")
        }

        shouldFail(RuntimeException) { abstractRoddyJob.failedOrNotFinishedClusterJobs([]) }
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenJobStateLogFileDoesNotExist_ShouldFail() {
        CreateJobStateLogFileHelper.withTmpRoddyExecutionDir(tmpDir, { roddyExecutionDir ->
            shouldFail(RuntimeException) { abstractRoddyJob.failedOrNotFinishedClusterJobs([]) }
        })
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenJobStateLogFileIsEmpty_ShouldFail() {
        CreateJobStateLogFileHelper.withJobStateLogFile(tmpDir, []) {
            shouldFailWithMessage(RuntimeException, /${it}\/${JobStateLogFile.JOB_STATE_LOG_FILE_NAME}\sis\sempty\./) {
                abstractRoddyJob.failedOrNotFinishedClusterJobs([])
            }
        }
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenJobStateLogFileIsCorrect_ShouldWork() {
        CreateJobStateLogFileHelper.withJobStateLogFile(tmpDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_STARTED])
        ]) {
            assert [(clusterJobIdentifier): "${clusterJobIdentifier} has not finished successfully. Status code: ${STATUS_CODE_STARTED}"] ==
                    abstractRoddyJob.failedOrNotFinishedClusterJobs([clusterJobIdentifier])
        }
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenSeveralJobStates_ShouldReturnCorrectMap() {
        ClusterJobIdentifier identifierA = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId1")
        ClusterJobIdentifier identifierB = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId2")
        ClusterJobIdentifier identifierC = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId3")
        ClusterJobIdentifier identifierD = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId4")

        // JOB A, 2 entries, statusCode = 0 => sucessfully finished job, no output,
        //                                   same identifier in older executionStore marked as failed, should be ignored
        // JOB B, 3 entires, statusCode != 0 => failed job
        // JOB C, 1 entry, statusCode = "57427" => still in progress
        // JOB D, 0 entries => no information found

        // jobStateLogFile for the first roddy call
        File firstRoddyExecDir = tmpDir.newFolder("exec_140625_102449388_SOMEUSER_WGS")
        CreateJobStateLogFileHelper.createJobStateLogFile(firstRoddyExecDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: identifierA.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: identifierA.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L])
        ])

        // create jobStateLogFile for the second roddy call and do test
        CreateJobStateLogFileHelper.withJobStateLogFile(tmpDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: identifierA.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: identifierA.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: identifierB.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: identifierB.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: identifierB.clusterJobId, statusCode: STATUS_CODE_FAILED, timeStamp: 100L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: identifierC.clusterJobId, statusCode: STATUS_CODE_STARTED])
        ], {
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs = [
                    identifierA,
                    identifierB,
                    identifierC,
                    identifierD,
            ]
            assert [
                    (identifierB): "${identifierB} has not finished successfully. Status code: ${STATUS_CODE_FAILED}",
                    (identifierC): "${identifierC} has not finished successfully. Status code: ${STATUS_CODE_STARTED}",
                    (identifierD): "JobStateLogFile contains no information for ${identifierD}",
            ] == abstractRoddyJob.failedOrNotFinishedClusterJobs(finishedClusterJobs)
        }, "exec_150625_102449388_SOMEUSER_WGS")
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenJobStateLogFileContainsNoInformationAboutPbsId_ShouldReturnSpecificErrorMsg() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [])

        assert [(clusterJobIdentifier): "JobStateLogFile contains no information for ${clusterJobIdentifier}"] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobIsInProgress_ShouldReturnSpecificErrorMsg() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_STARTED])
        ])

        assert [(clusterJobIdentifier): "${clusterJobIdentifier} has not finished successfully. Status code: ${STATUS_CODE_STARTED}"] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobFailedProcessing_ShouldReturnSpecificErrorMsg() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, timeStamp: 0L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, timeStamp: 10L, statusCode: STATUS_CODE_FAILED])
        ])

        assert [(clusterJobIdentifier): "${clusterJobIdentifier} has not finished successfully. Status code: ${STATUS_CODE_FAILED}"] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobFinishedSuccessfully_ShouldReturnEmptyMap() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED, timeStamp: 5L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([pbsId: clusterJobIdentifier.clusterJobId, timeStamp: 10L])
        ])

        assert [:] == abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenNoFinishedClusterJobs_ShouldReturnEmptyMap() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry()
        ])

        assert [:] == abstractRoddyJob.analyseFinishedClusterJobs([], jobStateLogFile)
    }
}
