package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
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
import org.junit.After
import org.junit.Before
import org.junit.Test

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
    final shouldFail = new GroovyTestCase().&shouldFail

    ClusterJobService clusterJobService

    AbstractRoddyJob roddyJob
    Realm realm
    int counter

    @Before
    void setUp() {
        counter = 0

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
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

        roddyJob.executeRoddyCommandService.metaClass.executeRoddyCommand = { String cmd ->
            return [ 'bash', '-c', "echo Hallo" ].execute()
        }
        roddyJob.executeRoddyCommandService.metaClass.returnStdoutOfFinishedCommandExecution = { Process process ->
            counter ++
            return "Running job r150428_104246480_stds_snvCallingMetaScript => 3504988"
        }
        roddyJob.executeRoddyCommandService.metaClass.checkIfRoddyWFExecutionWasSuccessful = { Process process ->
            counter++
        }
        roddyJob.log = log
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecuteRoddyCommandService, roddyJob.executeRoddyCommandService)
        TestCase.removeMetaClass(AbstractRoddyJob, roddyJob)
    }

    @Test
    void testMaybeSubmit() {
        assert AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS == roddyJob.maybeSubmit()
        assert counter == 2
    }

    @Test
    void testValidate() {
        roddyJob.validate()
        assert counter == 1
    }


    @Test
    void testExecute_finishedClusterJobsIsNull_MaybeSubmit() {
        roddyJob.metaClass.maybeSubmit = {
            return AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS
        }
        roddyJob.metaClass.validate = {
            throw new RuntimeException("should not come here")
        }

        assert AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS == roddyJob.execute(null)
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
        String roddyOutput = """\
Running job ${SNV_CALLING_META_SCRIPT_JOB_NAME} => ${SNV_CALLING_META_SCRIPT_PBSID}
Running job ${SNV_ANNOTATION_JOB_NAME} => ${SNV_ANNOTATION_PBSID}
Rerun job ${ALIGN_AND_PAIR_SLIM_JOB_NAME} => ${ALIGN_AND_PAIR_SLIM_PBSID}"""

        roddyJob.createClusterJobObjects(realm, roddyOutput)

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
        String roddyOutput = ""

        roddyJob.createClusterJobObjects(realm, roddyOutput)

        assert ClusterJob.all.empty
    }

    @Test
    void testCreateClusterJobObjects_skipLinesHavingOnlySpaces() {
        String roddyOutput = "     "

        roddyJob.createClusterJobObjects(realm, roddyOutput)

        assert ClusterJob.all.empty
    }

    @Test
    void testCreateClusterJobObjects_EntryHasSpacesAtTheStart() {
        String roddyOutput = "    Running job r150428_104246480_stds_snvCallingMetaScript => 3504988"

        roddyJob.createClusterJobObjects(realm, roddyOutput)

        assert 1 == ClusterJob.count()
    }

    @Test
    void testCreateClusterJobObjects_EntryHasTrailingSpaces() {
        String roddyOutput = "Running job r150428_104246480_stds_snvCallingMetaScript => 3504988    "

        roddyJob.createClusterJobObjects(realm, roddyOutput)

        assert 1 == ClusterJob.count()
    }

    @Test
    void testCreateClusterJobObjects_realmIsNull_fails() {
        String roddyOutput = "Running job r150428_104246480_stds_snvCallingMetaScript => 3504988"

        shouldFail AssertionError, {
            roddyJob.createClusterJobObjects(null, roddyOutput)
        }

        assert ClusterJob.all.empty
    }


    @Test
    void testCreateClusterJobObjects_invalidRoddyOutput_fails() {
        String roddyOutput = """\
asdfasdfasdf"""

        assert shouldFail(RuntimeException) {
            roddyJob.createClusterJobObjects(realm, roddyOutput)
        }.startsWith('Could not match')

        assert ClusterJob.all.empty
    }
}
