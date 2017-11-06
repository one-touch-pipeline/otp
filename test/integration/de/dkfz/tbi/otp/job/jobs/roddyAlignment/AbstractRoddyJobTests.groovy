package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.codehaus.groovy.control.io.NullWriter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static de.dkfz.tbi.TestConstants.ARBITRARY_MESSAGE
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class AbstractRoddyJobTests {

    public static final String SNV_CALLING_META_SCRIPT_PBSID = "3504988"
    public static final String SNV_CALLING_META_SCRIPT_JOB_NAME = "r150428_104246480_stds_snvCallingMetaScript"
    public static final String SNV_CALLING_META_SCRIPT_JOB_CLASS = "snvCallingMetaScript"
    public static final String SNV_ANNOTATION_PBSID = "3504989"
    public static final String SNV_ANNOTATION_JOB_NAME = "r150428_104246480_stds_snvAnnotation"
    public static final String SNV_ANNOTATION_JOB_CLASS = "snvAnnotation"
    public static final String ALIGN_AND_PAIR_SLIM_PBSID = "3744601"
    public static final String ALIGN_AND_PAIR_SLIM_JOB_NAME = "r150623_153422293_123456_alignAndPairSlim"
    public static final String ALIGN_AND_PAIR_SLIM_JOB_CLASS = "alignAndPairSlim"
    public static final String RODDY_EXECUTION_STORE_DIRECTORY_NAME = 'exec_150625_102449388_username_analysis'
    public static final ProcessOutput OUTPUT_CLUSTER_JOBS_SUBMITTED = new ProcessOutput(
            stderr: "",
            stdout: "Running job ${SNV_CALLING_META_SCRIPT_JOB_NAME} => ${SNV_CALLING_META_SCRIPT_PBSID}  \n" +
                    "\n" +
                    "  Running job ${SNV_ANNOTATION_JOB_NAME} => ${SNV_ANNOTATION_PBSID}\n" +
                    "  \n" +
                    "Rerun job ${ALIGN_AND_PAIR_SLIM_JOB_NAME} => ${ALIGN_AND_PAIR_SLIM_PBSID}",
            exitCode: 0,
    )
    public static final ProcessOutput OUTPUT_NO_CLUSTER_JOBS_SUBMITTED = new ProcessOutput(
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
        realm = Realm.build(
                name: roddyBamFile.project.realmName,
                operationType: Realm.OperationType.DATA_MANAGEMENT,
        )

        roddyJob = [
                getProcessParameterObject: { -> roddyBamFile },
                prepareAndReturnWorkflowSpecificCommand: { Object instance, Realm realm -> return "workflowSpecificCommand" },
                validate: { Object instance -> validateCounter ++ },
                getProcessingStep : { -> return DomainFactory.createAndSaveProcessingStep() },
        ] as AbstractRoddyJob

        roddyJob.executeRoddyCommandService = new ExecuteRoddyCommandService()
        roddyJob.configService = new ConfigService()
        roddyJob.clusterJobService = new ClusterJobService()
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecuteRoddyCommandService, roddyJob.executeRoddyCommandService)
        TestCase.removeMetaClass(AbstractRoddyJob, roddyJob)
        roddyBamFile = null
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }

    private File setRootPathAndCreateWorkExecutionStoreDirectory() {
        realm.rootPath = tmpDir.newFolder().path
        assert realm.save(failOnError: true)
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

        ProcessHelperService.metaClass.static.executeAndWait = { String cmd ->
            executeCommandCounter++
            return new ProcessHelperService.ProcessOutput(stdout: stdout, stderr: stderr, exitCode: 0)
        }

        return workExecutionDir
    }

    private File setUpWorkDirAndMockProcessOutputWithOutOfMemoryError() {
        File workExecutionDir = setRootPathAndCreateWorkExecutionStoreDirectory()

        stderr = """newLine
Creating the following execution directory to java.lang.OutOfMemoryError store information about this process:
${workExecutionDir.absolutePath}
newLine"""

        ProcessHelperService.metaClass.static.executeAndWait = { String cmd ->
            executeCommandCounter++
            return new ProcessHelperService.ProcessOutput(stdout: "", stderr: stderr, exitCode: 0)
        }

        return workExecutionDir
    }

    private void mockProcessOutput_noClusterJobsSubmitted() {
        ProcessHelperService.metaClass.static.executeAndWait = { String cmd ->
            executeCommandCounter++
            return OUTPUT_NO_CLUSTER_JOBS_SUBMITTED
        }
    }

    @Test
    void testMaybeSubmit_clusterJobsSubmitted() {
        setUpWorkDirAndMockProcessOutput()
        LogThreadLocal.withThreadLog(System.out) {
            assert AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS == roddyJob.maybeSubmit()
        }
        assert executeCommandCounter == 1
        assert validateCounter == 0
    }

    @Test
    void testMaybeSubmit_noClusterJobsSubmitted_validateSucceeds() {
        mockProcessOutput_noClusterJobsSubmitted()

        LogThreadLocal.withThreadLog(new NullWriter()) {
            assert AbstractMultiJob.NextAction.SUCCEED == roddyJob.maybeSubmit()
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
            return AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS
        }
        roddyJob.metaClass.validate = {
            throw new RuntimeException("should not come here")
        }
        LogThreadLocal.withThreadLog(System.out) {
            assert AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS == roddyJob.execute(null)
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
                failedOrNotFinishedClusterJobs: { Collection<? extends ClusterJobIdentifier> finishedClusterJobs -> [:] }
        ] as AbstractRoddyJob
        roddyJob.executeRoddyCommandService = new ExecuteRoddyCommandService()

        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()
        assert realm.save([flush: true, failOnError: true])

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep()
        assert processingStep

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, "0000", realm.unixUser, processingStep)
        assert clusterJob

        final ClusterJobIdentifier jobIdentifier = new ClusterJobIdentifier(realm, clusterJob.clusterJobId, realm.unixUser)

        roddyJob.metaClass.maybeSubmit = {
            throw new RuntimeException("should not come here")
        }

        assert AbstractMultiJob.NextAction.SUCCEED == roddyJob.execute([jobIdentifier])
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
        File workRoddyExecutionDir = setUpWorkDirAndMockProcessOutput()

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
