package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import org.codehaus.groovy.control.io.*
import org.junit.*
import org.junit.rules.*

import static de.dkfz.tbi.TestConstants.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class AbstractRoddyJobTests {

    static final String SNV_CALLING_META_SCRIPT_PBSID = "3504988"
    static final String SNV_CALLING_META_SCRIPT_JOB_NAME = "r150428_104246480_stds_snvCallingMetaScript"
    static final String SNV_CALLING_META_SCRIPT_JOB_CLASS = "snvCallingMetaScript"
    static final String SNV_ANNOTATION_PBSID = "3504989"
    static final String SNV_ANNOTATION_JOB_NAME = "r150428_104246480_stds_snvAnnotation"
    static final String SNV_ANNOTATION_JOB_CLASS = "snvAnnotation"
    static final String ALIGN_AND_PAIR_SLIM_PBSID = "3744601"
    static final String ALIGN_AND_PAIR_SLIM_JOB_NAME = "r150623_153422293_123456_alignAndPairSlim"
    static final String ALIGN_AND_PAIR_SLIM_JOB_CLASS = "alignAndPairSlim"
    static final String RODDY_EXECUTION_STORE_DIRECTORY_NAME = 'exec_150625_102449388_username_analysis'
    static final ProcessOutput OUTPUT_CLUSTER_JOBS_SUBMITTED = new ProcessOutput(
            stderr: "",
            stdout: "Running job ${SNV_CALLING_META_SCRIPT_JOB_NAME} => ${SNV_CALLING_META_SCRIPT_PBSID}  \n" +
                    "\n" +
                    "  Running job ${SNV_ANNOTATION_JOB_NAME} => ${SNV_ANNOTATION_PBSID}\n" +
                    "  \n" +
                    "Rerun job ${ALIGN_AND_PAIR_SLIM_JOB_NAME} => ${ALIGN_AND_PAIR_SLIM_PBSID}",
            exitCode: 0,
    )
    static final ProcessOutput OUTPUT_NO_CLUSTER_JOBS_SUBMITTED = new ProcessOutput(
            stderr: "Creating the following execution directory to store information about this process:\n" +
                    "\t${new File(TestCase.uniqueNonExistentPath, RODDY_EXECUTION_STORE_DIRECTORY_NAME)}" +
                    "${AbstractRoddyJob.NO_STARTED_JOBS_MESSAGE}",
            stdout: "",
            exitCode: 0,
    )

    final shouldFail = new GroovyTestCase().&shouldFail

    ClusterJobService clusterJobService

    AbstractRoddyJob roddyJob
    RoddyBamFile roddyBamFile
    Realm realm
    TestConfigService configService
    int executeCommandCounter
    int validateCounter

    String stderr

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        executeCommandCounter = 0
        validateCounter = 0

        roddyBamFile = DomainFactory.createRoddyBamFile()

        realm = roddyBamFile.project.realm
        configService = new TestConfigService()

        roddyJob = [
                getProcessParameterObject: { -> roddyBamFile },
                prepareAndReturnWorkflowSpecificCommand: { Object instance, Realm realm -> return "workflowSpecificCommand" },
                validate: { Object instance -> validateCounter ++ },
                getProcessingStep : { -> return DomainFactory.createAndSaveProcessingStep() },
        ] as AbstractRoddyJob

        roddyJob.executeRoddyCommandService = new ExecuteRoddyCommandService()
        roddyJob.configService = configService
        roddyJob.clusterJobService = new ClusterJobService()
        roddyJob.clusterJobSchedulerService = [
                retrieveAndSaveJobInformationAfterJobStarted: { ClusterJob clusterJob -> },
        ] as ClusterJobSchedulerService
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecuteRoddyCommandService, roddyJob.executeRoddyCommandService)
        TestCase.removeMetaClass(AbstractRoddyJob, roddyJob)
        roddyBamFile = null
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
        configService.clean()
    }

    private File setRootPathAndCreateWorkExecutionStoreDirectory() {
        configService.setOtpProperty((OtpProperty.PATH_PROJECT_ROOT), tmpDir.newFolder().path)
        File workRoddyExecutionDir =  new File(roddyBamFile.workExecutionStoreDirectory, RODDY_EXECUTION_STORE_DIRECTORY_NAME)
        assert workRoddyExecutionDir.mkdirs()
        return workRoddyExecutionDir
    }

    private File setUpWorkDirAndMockProcessOutput() {
        File workExecutionDir = setRootPathAndCreateWorkExecutionStoreDirectory()

        String stdout = "Running job abc_def => 3504988"
        stderr = """newLine
Creating the following execution directory to store information about this process:
${workExecutionDir.absolutePath}
newLine"""

        mockProcessOutput(stdout, stderr)

        return workExecutionDir
    }

    private File setUpWorkDirAndMockProcessOutputWithOutOfMemoryError() {
        File workExecutionDir = setRootPathAndCreateWorkExecutionStoreDirectory()

        stderr = """newLine
Creating the following execution directory to java.lang.OutOfMemoryError store information about this process:
${workExecutionDir.absolutePath}
newLine"""
        mockProcessOutput("", stderr)

        return workExecutionDir
    }

    private void mockProcessOutput(String output, String error) {
        roddyJob.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String cmd ->
                    executeCommandCounter++
                    return new ProcessOutput(stdout: output, stderr: error, exitCode: 0)
                }
        ] as RemoteShellHelper
    }

    private void mockProcessOutput_noClusterJobsSubmitted() {
        roddyJob.remoteShellHelper = [
                executeCommandReturnProcessOutput: { Realm realm, String cmd ->
                    executeCommandCounter++
                    return OUTPUT_NO_CLUSTER_JOBS_SUBMITTED
                }
        ]  as RemoteShellHelper
    }

    @Test
    void testMaybeSubmit_clusterJobsSubmitted() {
        setUpWorkDirAndMockProcessOutput()
        LogThreadLocal.withThreadLog(System.out) {
            assert NextAction.WAIT_FOR_CLUSTER_JOBS == roddyJob.maybeSubmit()
        }
        assert executeCommandCounter == 1
        assert validateCounter == 0
    }

    @Test
    void testMaybeSubmit_noClusterJobsSubmitted_validateSucceeds() {
        mockProcessOutput_noClusterJobsSubmitted()

        LogThreadLocal.withThreadLog(new NullWriter()) {
            assert NextAction.SUCCEED == roddyJob.maybeSubmit()
        }

        assert executeCommandCounter == 1
        assert validateCounter == 1
    }

    @Test
    void testMaybeSubmit_noClusterJobsSubmitted_validateFails() {
        mockProcessOutput_noClusterJobsSubmitted()
        roddyJob.metaClass.validate = { ->
            validateCounter++
            throw new RuntimeException(ARBITRARY_MESSAGE)
        }

        try {
            LogThreadLocal.withThreadLog(new NullWriter()) {
                roddyJob.maybeSubmit()
            }
            assert false : 'Should have thrown an exception.'
        } catch (Throwable t) {
            if (t.message != 'validate() failed after Roddy has not submitted any cluster jobs.' || t.cause?.message != ARBITRARY_MESSAGE) {
                throw t
            }
        }

        assert executeCommandCounter == 1
    }

    @Test
    void testValidate() {
        roddyJob.validate()
        assert validateCounter == 1
    }


    @Test
    void testExecute_finishedClusterJobsIsNull_MaybeSubmit() {
        setUpWorkDirAndMockProcessOutput()
        roddyJob.metaClass.maybeSubmit = {
            return NextAction.WAIT_FOR_CLUSTER_JOBS
        }
        roddyJob.metaClass.validate = {
            throw new RuntimeException("should not come here")
        }
        LogThreadLocal.withThreadLog(System.out) {
            assert NextAction.WAIT_FOR_CLUSTER_JOBS == roddyJob.execute(null)
        }
    }

    @Test
    void testMaybeSubmit_withOutOfMemoryError() {
        setUpWorkDirAndMockProcessOutputWithOutOfMemoryError()
        LogThreadLocal.withThreadLog(System.out) {
            final shouldFail = new GroovyTestCase().&shouldFail
            String error = shouldFail RuntimeException, {
                roddyJob.maybeSubmit()
            }
            assert error == "Out of memory error is found in Roddy"
        }
    }

    @Test
    void testExecute_finishedClusterJobsIsNull_Validate() {
        roddyJob = [
                validate: { -> validateCounter++ },
                failedOrNotFinishedClusterJobs: { Collection<? extends ClusterJobIdentifier> finishedClusterJobs -> [:] },
        ] as AbstractRoddyJob
        roddyJob.executeRoddyCommandService = new ExecuteRoddyCommandService()

        Realm realm = DomainFactory.createRealm()
        assert realm.save([flush: true, failOnError: true])

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep()
        assert processingStep

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, "0000", configService.getSshUser(), processingStep)
        assert clusterJob

        final ClusterJobIdentifier jobIdentifier = new ClusterJobIdentifier(realm, clusterJob.clusterJobId, configService.getSshUser())

        roddyJob.metaClass.maybeSubmit = {
            throw new RuntimeException("should not come here")
        }

        assert NextAction.SUCCEED == roddyJob.execute([jobIdentifier])
        assert executeCommandCounter == 0
        assert validateCounter == 1
    }


    @Test
    void testCreateClusterJobObjects_Works() {
        File workRoddyExecutionDir = setUpWorkDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(workRoddyExecutionDir.name)

        assert containSame(
                roddyJob.createClusterJobObjects(roddyBamFile, realm, OUTPUT_CLUSTER_JOBS_SUBMITTED),
                ClusterJob.all)

        assert ClusterJob.all.find {
            it.clusterJobId == SNV_CALLING_META_SCRIPT_PBSID &&
            it.clusterJobName == SNV_CALLING_META_SCRIPT_JOB_NAME &&
            it.jobClass == SNV_CALLING_META_SCRIPT_JOB_CLASS &&
            it.realm == realm &&
            !it.validated &&
            it.processingStep.id != null &&
            it.seqType.id != null &&
            it.queued != null &&
            it.exitStatus == null &&
            it.exitCode == null &&
            it.started == null &&
            it.ended == null &&
            it.requestedWalltime == null &&
            it.requestedCores == null &&
            it.usedCores == null &&
            it.cpuTime == null &&
            it.requestedMemory == null &&
            it.usedMemory == null
        }
        assert ClusterJob.all.find {
            it.clusterJobId == SNV_ANNOTATION_PBSID &&
            it.clusterJobName == SNV_ANNOTATION_JOB_NAME &&
            it.jobClass == SNV_ANNOTATION_JOB_CLASS &&
            it.realm == realm &&
            !it.validated &&
            it.processingStep.id != null &&
            it.seqType.id != null &&
            it.queued != null &&
            it.exitStatus == null &&
            it.exitCode == null &&
            it.started == null &&
            it.ended == null &&
            it.requestedWalltime == null &&
            it.requestedCores == null &&
            it.usedCores == null &&
            it.cpuTime == null &&
            it.requestedMemory == null &&
            it.usedMemory == null
        }
        assert ClusterJob.all.find {
            it.clusterJobId == ALIGN_AND_PAIR_SLIM_PBSID &&
            it.clusterJobName == ALIGN_AND_PAIR_SLIM_JOB_NAME &&
            it.jobClass == ALIGN_AND_PAIR_SLIM_JOB_CLASS &&
            it.realm == realm &&
            !it.validated &&
            it.processingStep.id != null &&
            it.seqType.id != null &&
            it.queued != null &&
            it.exitStatus == null &&
            it.exitCode == null &&
            it.started == null &&
            it.ended == null &&
            it.requestedWalltime == null &&
            it.requestedCores == null &&
            it.usedCores == null &&
            it.cpuTime == null &&
            it.requestedMemory == null &&
            it.usedMemory == null
        }
    }

    @Test
    void testCreateClusterJobObjects_noneSubmitted() {
        assert roddyJob.createClusterJobObjects(roddyBamFile, realm, OUTPUT_NO_CLUSTER_JOBS_SUBMITTED).empty
        assert ClusterJob.count() == 0
    }

    @Test
    void testCreateClusterJobObjects_realmIsNull_fails() {

        assert shouldFail(AssertionError) {
            roddyJob.createClusterJobObjects(roddyBamFile, null, OUTPUT_CLUSTER_JOBS_SUBMITTED)
        }.contains("assert realm")

        assert ClusterJob.all.empty
    }

    @Test
    void testCreateClusterJobObjects_roddyResultIsNull_fails() {

        assert shouldFail(AssertionError) {
            roddyJob.createClusterJobObjects(null, realm, OUTPUT_CLUSTER_JOBS_SUBMITTED)
        }.contains("assert roddyResult")

        assert ClusterJob.all.empty
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenRoddyResultIsNull_ShouldFail() {
        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(null, "")
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenParsedExecutionStoreDirNotEqualsToExpectedPath_ShouldFail() {
        setUpWorkDirAndMockProcessOutput()

        roddyBamFile.metaClass.getWorkExecutionStoreDirectory = {
            return tmpDir.newFolder("Folder")
        }

        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenExecutionStoreDirDoesNotExistOnFileSystem_ShouldFail() {
        File workRoddyExecutionDir = setUpWorkDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        workRoddyExecutionDir.delete()

        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenExecutionStoreDirIsNoDirectory_ShouldFail() {
        File workRoddyExecutionDir = setUpWorkDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        workRoddyExecutionDir.delete()
        tmpDir.newFile(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenLatestExecutionStoreDirIsNotLastElement_ShouldFail() {
        setUpWorkDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add("exec_999999_999999999_a_a")
        assert roddyBamFile.save(flush: true, failOnError: true)

        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenAllFine_ShouldBeOk() {
        setUpWorkDirAndMockProcessOutput()

        roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        assert roddyBamFile.roddyExecutionDirectoryNames.last() == RODDY_EXECUTION_STORE_DIRECTORY_NAME
    }
}
