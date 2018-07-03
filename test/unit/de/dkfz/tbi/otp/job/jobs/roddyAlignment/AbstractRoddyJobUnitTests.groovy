package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.buildtestdata.mixin.*
import org.junit.*
import org.junit.rules.*

import static de.dkfz.tbi.TestCase.*

@Build([
        ClusterJob,
        MergingCriteria,
        ProcessingOption,
        RoddyBamFile,
        Run,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SoftwareTool,
        MergingWorkPackage,
])
class AbstractRoddyJobUnitTests {

    public static final String STATUS_CODE_STARTED = "57427"
    public static final String STATUS_CODE_FINISHED = "0"
    public static final String STATUS_CODE_FAILED = "1"
    public static final String RANDOM_RODDY_EXECUTION_DIR = "/exec_150707_142149946_USER_WGS"

    AbstractRoddyJob abstractRoddyJob
    RoddyBamFile roddyBamFile
    ClusterJobIdentifier clusterJobIdentifier

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile(
                roddyExecutionDirectoryNames: [],
        )
        abstractRoddyJob = [processingStepId: 123456789, getProcessParameterObject: { roddyBamFile }] as AbstractRoddyJob
        clusterJobIdentifier = new ClusterJobIdentifier(DomainFactory.createRealm(), "clusterJobId", "userName")
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenRoddyExecutionDirectoryDoesNotExist_ShouldFail() {
        roddyBamFile.metaClass.getWorkExecutionStoreDirectory = { ->
            return TestCase.uniqueNonExistentPath
        }

        shouldFail(RuntimeException) { abstractRoddyJob.failedOrNotFinishedClusterJobs([]) }
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenJobStateLogFileDoesNotExist_ShouldFail() {
        CreateJobStateLogFileHelper.withWorkExecutionDir(tmpDir, { roddyExecutionDir ->
            shouldFail(RuntimeException) { abstractRoddyJob.failedOrNotFinishedClusterJobs([]) }
        })
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenJobStateLogFileIsCorrect_ShouldWork() {
        CreateJobStateLogFileHelper.withJobStateLogFile(tmpDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_STARTED])
        ]) {
            roddyBamFile.roddyExecutionDirectoryNames.add(it.name)
            roddyBamFile.save(flush: true)

            assert [(clusterJobIdentifier): "Status code: ${STATUS_CODE_STARTED}"] ==
                    abstractRoddyJob.failedOrNotFinishedClusterJobs([clusterJobIdentifier])
        }
    }

    @Test
    void testFailedOrNotFinishedClusterJobs_WhenSeveralJobStates_ShouldReturnCorrectMap() {
        Realm realm = DomainFactory.createRealm()
        ClusterJobIdentifier identifierA = new ClusterJobIdentifier(realm, "pbsId1", "userName")
        ClusterJobIdentifier identifierB = new ClusterJobIdentifier(realm, "pbsId2", "userName")
        ClusterJobIdentifier identifierC = new ClusterJobIdentifier(realm, "pbsId3", "userName")
        ClusterJobIdentifier identifierD = new ClusterJobIdentifier(realm, "pbsId4", "userName")

        // JOB A, 2 entries, statusCode = 0 => sucessfully finished job, no output,
        //                                   same identifier in older executionStore marked as failed, should be ignored
        // JOB B, 3 entires, statusCode != 0 => failed job
        // JOB C, 1 entry, statusCode = "57427" => still in progress
        // JOB D, 0 entries => no information found

        // jobStateLogFile for the first roddy call
        File firstRoddyExecDir = tmpDir.newFolder("exec_140625_102449388_SOMEUSER_WGS")
        CreateJobStateLogFileHelper.createJobStateLogFile(firstRoddyExecDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierA.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierA.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L])
        ])

        // create jobStateLogFile for the second roddy call and do test
        CreateJobStateLogFileHelper.withJobStateLogFile(tmpDir, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierA.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierA.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierB.clusterJobId, statusCode: STATUS_CODE_STARTED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierB.clusterJobId, statusCode: STATUS_CODE_FINISHED, timeStamp: 10L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierB.clusterJobId, statusCode: STATUS_CODE_FAILED, timeStamp: 100L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: identifierC.clusterJobId, statusCode: STATUS_CODE_STARTED])
        ], {
            roddyBamFile.roddyExecutionDirectoryNames.add(it.name)
            roddyBamFile.save(flush: true)

            Collection<? extends ClusterJobIdentifier> finishedClusterJobs = [
                    identifierA,
                    identifierB,
                    identifierC,
                    identifierD,
            ]
            assert [
                    (identifierB): "Status code: ${STATUS_CODE_FAILED}",
                    (identifierC): "Status code: ${STATUS_CODE_STARTED}",
                    (identifierD): "JobStateLogFile contains no information for this cluster job.",
            ] == abstractRoddyJob.failedOrNotFinishedClusterJobs(finishedClusterJobs)
        }, "exec_150625_102449388_SOMEUSER_WGS")
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenJobStateLogFileContainsNoInformationAboutJobId_ShouldReturnSpecificErrorMsg() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [])

        assert [(clusterJobIdentifier): "JobStateLogFile contains no information for this cluster job."] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobIsInProgress_ShouldReturnSpecificErrorMsg() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_STARTED])
        ])

        assert [(clusterJobIdentifier): "Status code: ${STATUS_CODE_STARTED}"] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobFailedProcessing_ShouldReturnSpecificErrorMsg() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, timeStamp: 0L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, timeStamp: 10L, statusCode: STATUS_CODE_FAILED])
        ])

        assert [(clusterJobIdentifier): "Status code: ${STATUS_CODE_FAILED}"] ==
                abstractRoddyJob.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFile)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobFinishedSuccessfully_ShouldReturnEmptyMap() {
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tmpDir.root, [
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, statusCode: STATUS_CODE_FAILED, timeStamp: 5L]),
                CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJobIdentifier.clusterJobId, timeStamp: 10L])
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

    @Test
    void testParseRoddyExecutionDirectoryFromRoddyOutput_WhenMatcherMatches_ShouldReturnRoddyExecutionDirectory() {
        String roddyExecutionDir = TestCase.uniqueNonExistentPath.absolutePath + RANDOM_RODDY_EXECUTION_DIR
        String output = """some String
Creating the following execution directory to store information about this process:
${roddyExecutionDir}
some String"""

        assert roddyExecutionDir == abstractRoddyJob.parseRoddyExecutionStoreDirectoryFromRoddyOutput(output).absolutePath
    }

    @Test
    void testParseRoddyExecutionDirectoryFromRoddyOutput_WhenMatcherDoesNotMatch_ShouldFail() {
        String output = "some wrong String"

        TestCase.shouldFail(RuntimeException) {
            abstractRoddyJob.parseRoddyExecutionStoreDirectoryFromRoddyOutput(output)
        }
    }

    @Test
    void testParseRoddyExecutionDirectoryFromRoddyOutput_WhenMatcherMatchesMoreThanOnce_ShouldFail() {
        String roddyExecutionDir = TestCase.uniqueNonExistentPath.absolutePath + RANDOM_RODDY_EXECUTION_DIR
        String output = """some String
Creating the following execution directory to store information about this process:
${roddyExecutionDir}
some String
Creating the following execution directory to store information about this process:
${roddyExecutionDir}
some String"""

        shouldFail(AssertionError) {
            abstractRoddyJob.parseRoddyExecutionStoreDirectoryFromRoddyOutput(output)
        }
    }
}
