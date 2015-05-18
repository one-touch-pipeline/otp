package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFiles
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import org.junit.After
import org.junit.Before
import org.junit.Test

class AbstractRoddyJobUnitTests extends TestCase {

    public static final String TEST_HOST = "testHost"
    public static final String TEST_EXIT_CODE = "testExitCode"
    public static final String TEST_JOB_CLASS = "testJobClass"

    def abstractMaybeSubmitValidateRoddyJobInst
    ClusterJobIdentifier clusterJobIdentifier
    CreateJobStateLogFileHelper createJobStateLogFileHelper = new CreateJobStateLogFileHelper()

    @Before
    void setUp() {
        abstractMaybeSubmitValidateRoddyJobInst = [processingStepId: 123456789] as AbstractRoddyJob
        clusterJobIdentifier = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId")
    }

    @After
    void tearDown() {
        removeMetaClass(AbstractRoddyJob, abstractMaybeSubmitValidateRoddyJobInst)
        GroovySystem.metaClassRegistry.removeMetaClass(RoddyBamFile)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenJobStateLogFileContainsNoInformationAboutPbsId_ShouldReturnSpecificErrorMsg() {
        JobStateLogFiles jobStateLogFiles = createJobStateLogFileHelper.createJobStateLogFiles()

        assert [(clusterJobIdentifier): "JobStateLogFile contains no information for ${clusterJobIdentifier}"] ==
                abstractMaybeSubmitValidateRoddyJobInst.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFiles)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobIsInProgress_ShouldReturnSpecificErrorMsg() {
        JobStateLogFiles jobStateLogFiles = createJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: TEST_EXIT_CODE,
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assert [(clusterJobIdentifier): "${clusterJobIdentifier} is not finished."] ==
                abstractMaybeSubmitValidateRoddyJobInst.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFiles)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobFailedProcessing_ShouldReturnSpecificErrorMsg() {
        JobStateLogFiles jobStateLogFiles = createJobStateLogFileHelper.createJobStateLogFiles([
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
                        timeStamp: "10",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assert [(clusterJobIdentifier): "${clusterJobIdentifier} failed processing. ExitCode: ${jobStateLogFiles.getPropertyFromLatestLogFileEntry(clusterJobIdentifier.clusterJobId, "exitCode")}"] ==
                abstractMaybeSubmitValidateRoddyJobInst.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFiles)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenClusterJobFinishedSuccessfully_ShouldReturnEmptyMap() {
        JobStateLogFiles jobStateLogFiles = createJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: "0",
                        timeStamp: "10",
                        jobClass: TEST_JOB_CLASS
                ],
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
                        timeStamp: "5",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assert [:] == abstractMaybeSubmitValidateRoddyJobInst.analyseFinishedClusterJobs([clusterJobIdentifier], jobStateLogFiles)
    }

    @Test
    void testAnalyseFinishedClusterJobs_WhenNoFinishedClusterJobs_ShouldReturnEmptyMap() {
        JobStateLogFiles jobStateLogFiles = createJobStateLogFileHelper.createJobStateLogFiles([
                [
                        pbsId: clusterJobIdentifier.clusterJobId,
                        host: TEST_HOST,
                        exitCode: TEST_EXIT_CODE,
                        timeStamp: "0",
                        jobClass: TEST_JOB_CLASS
                ]
        ])

        assert [:] == abstractMaybeSubmitValidateRoddyJobInst.analyseFinishedClusterJobs([], jobStateLogFiles)
    }
}
