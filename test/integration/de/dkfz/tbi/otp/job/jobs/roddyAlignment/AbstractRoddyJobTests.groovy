package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.scheduler.PbsJobInfo
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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

    final shouldFail = new GroovyTestCase().&shouldFail

    ClusterJobService clusterJobService

    AbstractRoddyJob roddyJob
    RoddyBamFile roddyBamFile
    Realm realm
    int counter

    String stderr

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        counter = 0

        tmpDir.create()

        roddyBamFile = DomainFactory.createRoddyBamFile()
        realm = Realm.build([name: roddyBamFile.project.realmName, operationType: Realm.OperationType.DATA_MANAGEMENT])

        roddyJob = [
                getProcessParameterObject: { -> roddyBamFile },
                prepareAndReturnWorkflowSpecificCommand: { Object instance, Realm realm -> return "workflowSpecificCommand" },
                validate: { Object instance -> counter ++ },
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

    private File setRootPathAndCreateTmpRoddyExecutionStoreDirectory() {
        realm.rootPath = tmpDir.newFolder().path
        assert realm.save(failOnError: true)
        File tmpRoddyExecutionDir =  new File(roddyBamFile.tmpRoddyExecutionStoreDirectory, RODDY_EXECUTION_STORE_DIRECTORY_NAME)
        assert tmpRoddyExecutionDir.mkdirs()
        return tmpRoddyExecutionDir
    }

    private File setUpTmpDirAndMockProcessOutput() {
        File tmpRoddyExecutionDir = setRootPathAndCreateTmpRoddyExecutionStoreDirectory()

        String stdout = "Running job abc_def => 3504988"
        stderr = """newLine
Creating the following execution directory to store information about this process:
${tmpRoddyExecutionDir.absolutePath}
newLine"""

        ProcessHelperService.metaClass.static.executeCommandAndAssertExistCodeAndReturnProcessOutput = {String cmd ->
            counter++
            return new ProcessHelperService.ProcessOutput(stdout: stdout, stderr: stderr, exitCode: 0)
        }

        return tmpRoddyExecutionDir
    }

    @Test
    void testMaybeSubmit() {
        setUpTmpDirAndMockProcessOutput()
        LogThreadLocal.withThreadLog(System.out) {
            assert AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS == roddyJob.maybeSubmit()
        }
        assert counter == 1
    }

    @Test
    void testValidate() {
        roddyJob.validate()
        assert counter == 1
    }


    @Test
    void testExecute_finishedClusterJobsIsNull_MaybeSubmit() {
        setUpTmpDirAndMockProcessOutput()
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
    void testExecute_finishedClusterJobsIsNull_Validate() {
        roddyJob = [
                validate: { -> counter ++ },
                failedOrNotFinishedClusterJobs: { Collection<? extends ClusterJobIdentifier> finishedClusterJobs -> [:] }
        ] as AbstractRoddyJob
        roddyJob.executeRoddyCommandService = new ExecuteRoddyCommandService()

        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()
        assert realm.save([flush: true, failOnError: true])

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep()
        assert processingStep

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, "0000", processingStep)
        assert clusterJob

        final PbsJobInfo pbsJobInfo = new PbsJobInfo(realm: realm, pbsId: clusterJob.clusterJobId)

        roddyJob.metaClass.maybeSubmit = {
            throw new RuntimeException("should not come here")
        }

        assert AbstractMultiJob.NextAction.SUCCEED == roddyJob.execute([pbsJobInfo])
        assert counter == 1
    }


    @Test
    void testCreateClusterJobObjects_Works() {
        File tmpRoddyExecutionDir = setUpTmpDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(tmpRoddyExecutionDir.name)

        String stdout = """\
Running job ${SNV_CALLING_META_SCRIPT_JOB_NAME} => ${SNV_CALLING_META_SCRIPT_PBSID}
Running job ${SNV_ANNOTATION_JOB_NAME} => ${SNV_ANNOTATION_PBSID}
Rerun job ${ALIGN_AND_PAIR_SLIM_JOB_NAME} => ${ALIGN_AND_PAIR_SLIM_PBSID}"""

        roddyJob.createClusterJobObjects(roddyBamFile, realm, stdout)

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
    void testCreateClusterJobObjects_skipEmptyLines() {
        String stdout = ""

        roddyJob.createClusterJobObjects(roddyBamFile, realm, stdout)

        assert ClusterJob.all.empty
    }

    @Test
    void testCreateClusterJobObjects_skipLinesHavingOnlySpaces() {
        String stdout = "     "

        roddyJob.createClusterJobObjects(roddyBamFile, realm, stdout)

        assert ClusterJob.all.empty
    }

    @Test
    void testCreateClusterJobObjects_EntryHasSpacesAtTheStart() {
        File tmpRoddyExecutionDir = setUpTmpDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(tmpRoddyExecutionDir.name)

        String stdout = "    Running job r150428_104246480_stds_snvCallingMetaScript => 3504988"

        roddyJob.createClusterJobObjects(roddyBamFile, realm, stdout)

        assert 1 == ClusterJob.count()
    }

    @Test
    void testCreateClusterJobObjects_EntryHasTrailingSpaces() {
        File tmpRoddyExecutionDir = setUpTmpDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(tmpRoddyExecutionDir.name)

        String stdout = "Running job r150428_104246480_stds_snvCallingMetaScript => 3504988    "

        roddyJob.createClusterJobObjects(roddyBamFile, realm, stdout)

        assert 1 == ClusterJob.count()
    }

    @Test
    void testCreateClusterJobObjects_realmIsNull_fails() {
        String stdout = "Running job r150428_104246480_stds_snvCallingMetaScript => 3504988"

        assert shouldFail(AssertionError) {
            roddyJob.createClusterJobObjects(roddyBamFile, null, stdout)
        }.contains("assert realm")

        assert ClusterJob.all.empty
    }

    @Test
    void testCreateClusterJobObjects_roddyResultIsNull_fails() {
        String stdout = "Running job r150428_104246480_stds_snvCallingMetaScript => 3504988"

        assert shouldFail(AssertionError) {
            roddyJob.createClusterJobObjects(null, realm, stdout)
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
        File tmpRoddyExecutionDir = setUpTmpDirAndMockProcessOutput()

        roddyBamFile.metaClass.getTmpRoddyExecutionStoreDirectory = {
            return tmpDir.newFolder("Folder")
        }

        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenExecutionStoreDirDoesNotExistOnFileSystem_ShouldFail() {
        File tmpRoddyExecutionDir = setUpTmpDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        tmpRoddyExecutionDir.delete()

        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenExecutionStoreDirIsNoDirectory_ShouldFail() {
        File tmpRoddyExecutionDir = setUpTmpDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        tmpRoddyExecutionDir.delete()
        tmpDir.newFile(RODDY_EXECUTION_STORE_DIRECTORY_NAME)

        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenLatestExecutionStoreDirIsNotLastElement_ShouldFail() {
        setUpTmpDirAndMockProcessOutput()

        roddyBamFile.roddyExecutionDirectoryNames.add("exec_999999_999999999_a_a")

        shouldFail(AssertionError) {
            roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        }
    }

    @Test
    void testSaveRoddyExecutionStoreDirectory_WhenAllFine_ShouldBeOk() {
        setUpTmpDirAndMockProcessOutput()

        roddyJob.saveRoddyExecutionStoreDirectory(roddyBamFile as RoddyResult, stderr)
        assert roddyBamFile.roddyExecutionDirectoryNames.last() == RODDY_EXECUTION_STORE_DIRECTORY_NAME
    }
}
