package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.scheduler.PbsJobInfo
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.After
import org.junit.Before
import org.junit.Test

class AbstractRoddyJobTests {

    public static final ClusterJobIdentifier identifierA = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId1")
    public static final ClusterJobIdentifier identifierB = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId2")
    public static final ClusterJobIdentifier identifierC = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId3")
    public static final ClusterJobIdentifier identifierD = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId4")
    public static final ClusterJobIdentifier identifierE = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId5")
    public static final ClusterJobIdentifier identifierF = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId6")
    public static final ClusterJobIdentifier identifierG = new ClusterJobIdentifierImpl(DomainFactory.createRealmDataProcessingDKFZ(), "pbsId7")
    final shouldFail = new GroovyTestCase().&shouldFail

    ClusterJobService clusterJobService
    File executionStore

    AbstractRoddyJob roddyJob
    Realm realm
    int counter

    @Before
    void setUp() {
        counter = 0

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        realm = Realm.build([name: roddyBamFile.project.realmName, operationType: Realm.OperationType.DATA_MANAGEMENT])


        executionStore =  roddyBamFile.getTmpRoddyExecutionStoreDirectory()
        assert executionStore.mkdirs()

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
    }

    @After
    void tearDown() {
        assert executionStore.deleteDir()
        TestCase.removeMetaClass(ExecuteRoddyCommandService, roddyJob.executeRoddyCommandService)
        TestCase.removeMetaClass(AbstractRoddyJob, roddyJob)
    }

    @Test
    void testFailedOrNotFinishedClusterJobs() {
        Map<String, String> logFileMapA = createLogFileMap(identifierA, [exitCode: "1", timeStamp: "10"])
        Map<String, String> logFileMapB = createLogFileMap(identifierB, [exitCode: "4", timeStamp: "10"])
        Map<String, String> logFileMapC = createLogFileMap(identifierC, [exitCode: "3", timeStamp: "10"])
        Map<String, String> logFileMapE = createLogFileMap(identifierE, [exitCode: "0", timeStamp: "10"])
        Map<String, String> logFileMapF = createLogFileMap(identifierF, [exitCode: "1", timeStamp: "10"])
        Map<String, String> logFileMapG = createLogFileMap(identifierG, [exitCode: "1", timeStamp: "10"])

        // JOB A, 2 entries (start-entry + end-entry) in FILE 1, exitCode != 0 => failed
        // JOB B, 3 entries (start-entry + changed-entry + end-entry) in FILE 1, exitCode != 0 => failed
        // JOB C, 1 entry (start-entry) in FILE 1 => still in progress
        // JOB D, 0 enties => no information found
        // JOB E, 2 entries (start-entry + end-entry) in FILE 1, exitCode = 0 => sucessfully finished job, no output
        // JOB F, 2 entries (start-entry + end-entry) in FILE 2, exitCode != 0 => failed
        // JOB G, 2 entries (start-entry + end-entry) in FILE 1 + FILE 2, exitCode != 0 => failed

        String content = """
${logFileMapA.pbsId}.${logFileMapA.host}:57427:0:${logFileMapA.jobClass}
${logFileMapB.pbsId}.${logFileMapB.host}:57427:0:${logFileMapB.jobClass}
${logFileMapC.pbsId}.${logFileMapC.host}:57427:0:${logFileMapC.jobClass}
${logFileMapE.pbsId}.${logFileMapE.host}:57427:0:${logFileMapE.jobClass}
${logFileMapA.pbsId}.${logFileMapA.host}:${logFileMapA.exitCode}:${logFileMapA.timeStamp}:${logFileMapA.jobClass}
${logFileMapB.pbsId}.${logFileMapB.host}:0:1:${logFileMapB.jobClass}
${logFileMapB.pbsId}.${logFileMapB.host}:${logFileMapB.exitCode}:${logFileMapB.timeStamp}:${logFileMapB.jobClass}
${logFileMapE.pbsId}.${logFileMapE.host}:${logFileMapE.exitCode}:${logFileMapE.timeStamp}:${logFileMapE.jobClass}
${logFileMapG.pbsId}.${logFileMapG.host}:57427:0:${logFileMapG.jobClass}
"""
        String content2 = """
${logFileMapF.pbsId}.${logFileMapF.host}:57427:0:${logFileMapF.jobClass}
${logFileMapF.pbsId}.${logFileMapF.host}:${logFileMapF.exitCode}:${logFileMapF.timeStamp}:${logFileMapF.jobClass}
${logFileMapG.pbsId}.${logFileMapG.host}:${logFileMapG.exitCode}:${logFileMapG.timeStamp}:${logFileMapG.jobClass}
"""

        CreateFileHelper.createFile(new File(executionStore, "testFile"), content)
        CreateFileHelper.createFile(new File(executionStore, "testFile2"), content2)

        Collection<? extends ClusterJobIdentifier> finishedClusterJobs =
                [
                        identifierA,
                        identifierB,
                        identifierC,
                        identifierD,
                        identifierE,
                        identifierF,
                        identifierG
                ]

        assert [
                (identifierA): "${identifierA} failed processing. ExitCode: ${logFileMapA.exitCode}",
                (identifierB): "${identifierB} failed processing. ExitCode: ${logFileMapB.exitCode}",
                (identifierC): "${identifierC} is not finished.",
                (identifierD): "JobStateLogFile contains no information for ${identifierD}",
                (identifierF): "${identifierF} failed processing. ExitCode: ${logFileMapF.exitCode}",
                (identifierG): "${identifierG} failed processing. ExitCode: ${logFileMapG.exitCode}",
        ] == roddyJob.failedOrNotFinishedClusterJobs(finishedClusterJobs)
    }


    private Map<String, String> createLogFileMap(ClusterJobIdentifier identifier, properties = [:]) {
        return [pbsId: identifier.clusterJobId,
                host: "testHost",
                exitCode: "0",
                timeStamp: "0",
                jobClass: "testJobClass"] + properties
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
Running job r150428_104246480_stds_snvCallingMetaScript => 3504988
Running job r150428_104246480_stds_snvAnnotation => 3504989"""


        roddyJob.createClusterJobObjects(realm, roddyOutput)

        assert ClusterJob.all.find {
            it.clusterJobId == "3504988" &&
            it.clusterJobName == "snvCallingMetaScript_AbstractRoddyJob_groovyProxy" &&
            it.jobClass == "AbstractRoddyJob_groovyProxy" &&
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
            it.clusterJobId == "3504989" &&
            it.clusterJobName == "snvAnnotation_AbstractRoddyJob_groovyProxy" &&
            it.jobClass == "AbstractRoddyJob_groovyProxy" &&
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

        shouldFail IllegalStateException, {
            roddyJob.createClusterJobObjects(realm, roddyOutput)
        }

        assert ClusterJob.all.empty
    }
}
