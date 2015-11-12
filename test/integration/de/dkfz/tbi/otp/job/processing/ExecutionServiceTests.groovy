package de.dkfz.tbi.otp.job.processing

import org.apache.commons.logging.impl.NoOpLog

import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.TestCase

import static org.junit.Assert.*
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.job.jobs.TestJob
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType
import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import static de.dkfz.tbi.otp.job.jobs.TestJobHelper.*

class ExecutionServiceTests extends AbstractIntegrationTest {

    public static final String UNKNOWN_JOB_ID = '22'
    ExecutionService executionService
    GrailsApplication grailsApplication
    SchedulerService schedulerService

    Realm realm

    final static String SCRIPT_CONTENT = """#!/bin/bash
                       date
                       sleep 12
                       """
    final static String QSUB_PARAMETERS = "{-l: {walltime: '00:05:00'}}"

    @Before
    void setUp() {
        realm = new Realm(
                name: "DKFZ",
                env: "development",
                operationType: OperationType.DATA_MANAGEMENT,
                cluster: Realm.Cluster.DKFZ,
                rootPath: "/dev/null/otp-test/project/",
                processingRootPath: "/dev/null/otp-test/processing/",
                loggingRootPath: "/dev/null/otp-test/logging/",
                programsRootPath: "/testPrograms",
                webHost: "http://test.me",
                host: grailsApplication.config.otp.pbs.ssh.host,
                port: 22,
                unixUser: grailsApplication.config.otp.pbs.ssh.unixUser,
                timeout: 100,
                pbsOptions: "{-l: {nodes: '1', walltime: '00:00:30'}}"
                )
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options -> return ['1234.example.pbs.server.invalid'] }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecutionService, executionService)
    }

    @Test
    void testExecuteCommand_WhenRealmIsNull_ShouldFail() {
        assertNotNull(realm.save())
        shouldFail(NullPointerException) {
            executionService.executeCommand(null, SCRIPT_CONTENT)
        }
    }

    @Test
    void testExecuteCommand_WhenCommandIsNull_ShouldFail() {
        assertNotNull(realm.save())
        shouldFail(ProcessingException) {
            executionService.executeCommand(realm, null)
        }
    }

    @Deprecated
    @Ignore("OTP-1423")
    @Test
    void testExecuteJobOnlyScript() {
        assertNotNull(realm.save())
        TestJob testJob = createTestJobWithProcessingStep(DomainFactory.createSeqTrack())
        testJob.log = new NoOpLog()
        schedulerService.startingJobExecutionOnCurrentThread(testJob)
        try {
            // Send script to pbs
            String response = executionService.executeJob(realm, SCRIPT_CONTENT)
            // Extract pbs ids
            List<String> extractedPbsIds = executionService.extractPbsIds(response)
            assertNotNull(extractedPbsIds)
            // Only one pbs id is set
            String extractedPbsId = extractedPbsIds.get(0)
            // Make new pbs command to verify whether pbs job still is running
            String cmd = "qstat ${extractedPbsId}"
            // Send verifying command with recent pbs id to pbs
            String extractedPbsId_qstat = getPbsIdForExecutedCommand(realm, cmd)
            // Assert if the two extracted pbs ids are equal
            assertEquals(extractedPbsId, extractedPbsId_qstat)
        }
        finally {
            schedulerService.finishedJobExecutionOnCurrentThread(testJob)
        }
    }

    @Test
    void testExecuteJob_WhenSshServerDoesNotReturnAPbsId_ShouldThrow() {
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options -> return [] }
        assertNotNull(realm.save())
        TestJob testJob = createTestJobWithProcessingStep(DomainFactory.createSeqTrack())
        testJob.log = new NoOpLog()
        schedulerService.startingJobExecutionOnCurrentThread(testJob)
        try {
            String message = new GroovyTestCase().shouldFail(RuntimeException) {
                executionService.executeJob(realm, SCRIPT_CONTENT)
            }
            assert "Could not extract exactly one pbs id from ''" == message
        }
        finally {
            schedulerService.finishedJobExecutionOnCurrentThread(testJob)
        }
    }

    @Deprecated
    @Ignore("OTP-1423")
    @Test
    void testExecuteJobScriptAndJobIdentifier() {
        assertNotNull(realm.save())
        TestJob testJob = createTestJobWithProcessingStep(DomainFactory.createSeqTrack())
        testJob.log = new NoOpLog()
        schedulerService.startingJobExecutionOnCurrentThread(testJob)
        try {
            ProcessingOption processingOption = new ProcessingOption(
                    name: PbsOptionMergingService.PBS_PREFIX + JOB_IDENTFIER,
                    type: Realm.Cluster.DKFZ.toString(),
                    value: "{-l: {walltime: '00:01:00'}}",
                    comment: 'comment'
                    )
            assertNotNull(processingOption.save())
            // Send script to pbs
            String response = executionService.executeJob(realm, SCRIPT_CONTENT)
            // Extract pbs ids
            List<String> extractedPbsIds = executionService.extractPbsIds(response)
            assertNotNull(extractedPbsIds)
            // Only one pbs id is set
            String extractedPbsId = extractedPbsIds.get(0)
            // Make new pbs command to verify whether pbs job still is running
            String cmd = "qstat ${extractedPbsId}"
            // Send verifying command with recent pbs id to pbs
            String extractedPbsId_qstat = getPbsIdForExecutedCommand(realm, cmd)
            // Assert if the two extracted pbs ids are equal
            assertEquals(extractedPbsId, extractedPbsId_qstat)
        }
        finally {
            schedulerService.finishedJobExecutionOnCurrentThread(testJob)
        }
    }

    @Deprecated
    @Ignore("OTP-1423")
    @Test
    void testExecuteJobScriptAndJobIdentifierAndQsubParameter() {
        assertNotNull(realm.save())
        TestJob testJob = createTestJobWithProcessingStep(DomainFactory.createSeqTrack())
        testJob.log = new NoOpLog()
        schedulerService.startingJobExecutionOnCurrentThread(testJob)
        try {
            ProcessingOption processingOption = new ProcessingOption(
                    name: PbsOptionMergingService.PBS_PREFIX + JOB_IDENTFIER,
                    type: Realm.Cluster.DKFZ.toString(),
                    value: "{-l: {walltime: '00:01:00'}}",
                    comment: 'comment'
                    )
            assertNotNull(processingOption.save())
            // Send script to pbs
            String response = executionService.executeJob(realm, SCRIPT_CONTENT, QSUB_PARAMETERS)
            // Extract pbs ids
            List<String> extractedPbsIds = executionService.extractPbsIds(response)
            assertNotNull(extractedPbsIds)
            // Only one pbs id is set
            String extractedPbsId = extractedPbsIds.get(0)
            // Make new pbs command to verify whether pbs job still is running
            String cmd = "qstat ${extractedPbsId}"
            // Send verifying command with recent pbs id to pbs
            String extractedPbsId_qstat = getPbsIdForExecutedCommand(realm, cmd)
            // Assert if the two extracted pbs ids are equal
            assertEquals(extractedPbsId, extractedPbsId_qstat)
        }
        finally {
            schedulerService.finishedJobExecutionOnCurrentThread(testJob)
        }
    }

    @Test
    void testExecuteCommandConnectionFailed() {
        TestCase.removeMetaClass(ExecutionService, executionService)
        realm.host = "test.host.invalid"
        assertNotNull(realm.save())
        // Send command to pbs
        shouldFail(ProcessingException) {
            executionService.executeRemoteJob(realm, SCRIPT_CONTENT)
        }
    }

    @Test
    void testCheckRunningJob_WhenJobIsFoundStatusCompleted_ShouldReturnFalse() {
        assertNotNull(realm.save())
        executionService.metaClass.executeCommand { Realm r, String s ->
            return """
Job id                    Name             User            Time Use S Queue
------------------------- ---------------- --------------- -------- - -----
X.headnode                test             test                   0 C verylong
"""
        }
        assertFalse(executionService.checkRunningJob("X", realm))
    }

    @Test
    void testCheckRunningJob_WhenJobIsFoundStatusNotCompleted_ShouldReturnTrue() {
        assertNotNull(realm.save())
        executionService.metaClass.executeCommand { Realm r, String s ->
            return """
Job id                    Name             User            Time Use S Queue
------------------------- ---------------- --------------- -------- - -----
X.headnode                test             test                   0 Q verylong
"""
        }
        assertTrue(executionService.checkRunningJob("X", realm))
    }

    @Test
    void testCheckRunningJob_WhenJobIsFoundStatusCompletedNewFormat_ShouldReturnFalse() {
        assertNotNull(realm.save())
        executionService.metaClass.executeCommand { Realm r, String s ->
            return """
Job ID                    Name             User            Time Use S Queue
------------------------- ---------------- --------------- -------- - -----
X.headnode          ...QaAnalysisJob klinga          00:00:00 C fast
"""
        }
        assertFalse(executionService.checkRunningJob("X", realm))
    }

    @Test
    void testCheckRunningJob_WhenJobIsFoundStatusNotCompletedNewFormat_ShouldReturnFalse() {
        assertNotNull(realm.save())
        executionService.metaClass.executeCommand { Realm r, String s ->
            return """
Job ID                    Name             User            Time Use S Queue
------------------------- ---------------- --------------- -------- - -----
X.headnode          ...QaAnalysisJob klinga          00:00:00 R fast
"""
        }
        assertTrue(executionService.checkRunningJob("X", realm))
    }

    @Test
    void testCheckRunningJob_WhenJobIsNotFoundOldFormat_ShouldReturnFalse() {
        assertNotNull(realm.save())
        //
        executionService.metaClass.executeCommand { Realm r, String s ->
            return "qstat: Unknown Job Id 22.clust_node.long-domain"
        }
        assertFalse(executionService.checkRunningJob("X", realm))
    }

    @Test
    void testCheckRunningJob_WhenJobIsNotFoundNewFormat_ShouldReturnFalse() {
        assertNotNull(realm.save())
        executionService.metaClass.executeCommand { Realm r, String s ->
            return "qstat: Unknown Job Id Error 22.headnode.long-domain"
        }
        assertFalse(executionService.checkRunningJob("X", realm))
    }


    @Test
    void testCheckRunningJob_WhenQStatReturnedEmptyString_ShouldReturnTrue() {
        assertNotNull(realm.save())
        executionService.metaClass.executeCommand { Realm r, String s ->
            return ""
        }
        assertTrue(executionService.checkRunningJob("", realm))
    }

    @Test
    void testCheckRunningJob_WhenErrorDuringRequest_ShouldReturnTrue() {
        assertNotNull(realm.save())
        executionService.metaClass.executeCommand { Realm r, String s ->
            throw new Exception()
        }
        assertTrue(executionService.checkRunningJob("", realm))
    }

    @Ignore('Due unmocked cluster connection')
    @Test
    void testCheckRunningJob_WhenJobIsNotFound_ShouldReturnFalse() {
        TestCase.removeMetaClass(ExecutionService, executionService)

        realm = new Realm(
                name: "DKFZ",
                env: "development",
                operationType: OperationType.DATA_MANAGEMENT,
                cluster: Realm.Cluster.DKFZ,
                rootPath: "$OTP_ROOT_PATH/",
                processingRootPath: "STORAGE_ROOT/analysis/",
                loggingRootPath: "STORAGE_ROOT/dmg/otp/production/staging/log/",
                programsRootPath: "/",
                webHost: "https://otp.local/ngsdata/",
                host: 'headnode',
                port: 22,
                unixUser: 'otptest',
                timeout: 0,
                pbsOptions: "{-l: {nodes: '1', walltime: '00:00:30'}}"
        )
        assertNotNull(realm.save())

        assertFalse(executionService.checkRunningJob(UNKNOWN_JOB_ID, realm))
    }

    /**
     * The method executes the given command on the cluster and returns the PBS ID.
     */
    String getPbsIdForExecutedCommand(Realm realm, String cmd) {
        String response = executionService.executeJob(realm, cmd)
        // Extract pbs ids
        List<String> extractedPbsIds = executionService.extractPbsIds(response)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        assertEquals(1, extractedPbsIds.size())
        return extractedPbsIds.get(0)
    }
}
