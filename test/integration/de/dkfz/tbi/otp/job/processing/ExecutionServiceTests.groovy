package de.dkfz.tbi.otp.job.processing


import de.dkfz.tbi.otp.dataprocessing.AlignmentPass
import de.dkfz.tbi.TestCase

import static org.junit.Assert.*
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.job.jobs.TestJob
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import static de.dkfz.tbi.otp.job.jobs.TestJobHelper.*

class ExecutionServiceTests extends AbstractIntegrationTest {

    ExecutionService executionService
    GrailsApplication grailsApplication
    SchedulerService schedulerService

    Realm realm

    final static String SCRIPT_CONTENT = """#!/bin/bash
                       date
                       sleep 12
                       """
    final static String QSUB_PARAMETERS = "{-l: {walltime: '00:05:00'}}"
    final static String JOB_IDENTFIER = 'job'

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
                pbsOptions: "{-l: {nodes: '1:lsdf', walltime: '00:00:30'}}"
                )
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options -> return ['1234.example.pbs.server.invalid'] }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecutionService, executionService)
    }


    @Test
    void testExecuteCommand() {
        assertNotNull(realm.save())
        // Neither a command nor a script specified to be run remotely.
        shouldFail(ProcessingException) {
            executionService.executeCommand(realm, null)
        }
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(SCRIPT_CONTENT)
        // File has to be executable
        cmdFile.setExecutable(true)
        // Construct pbs command
        String cmd = "qsub ${cmdFile.name}"
        // No valid realm specified.
        shouldFail(NullPointerException) {
            executionService.executeCommand(null, cmd)
        }
        // Send command to pbs
        String response = executionService.executeCommand(realm, cmd)
        // Extract pbs ids
        List<String> extractedPbsIds = executionService.extractPbsIds(response)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        String extractedPbsId = extractedPbsIds.get(0)
        // Make new pbs command to verify whether pbs job still is running
        cmd = "qstat ${extractedPbsId}"
        // Send verifying command with recent pbs id to pbs
        String extractedPbsId_qstat = getPbsIdForExecutedCommand(realm, cmd)
        // Assert if the two extracted pbs ids are equal
        assertEquals(extractedPbsId, extractedPbsId_qstat)
    }

    @Deprecated
    @Test
    void testExecuteJobScript() {
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File file = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        file.setText(SCRIPT_CONTENT)
        file.setExecutable(true)
        // No valid realm specified.
        shouldFail(NullPointerException) {
            executionService.executeJobScript(null, file.absolutePath)
        }
        // Send file path to pbs
        String response = executionService.executeJobScript(realm, file.absolutePath)
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


    @Test
    void testExecuteJobOnlyScript() {
        assertNotNull(realm.save())
        TestJob testJob = createTestJobWithProcessingStep(AlignmentPass.build())
        testJob.log = log
        schedulerService.startingJobExecutionOnCurrentThread(testJob)
        try {
            String script = SCRIPT_CONTENT
            // Send script to pbs
            String response = executionService.executeJob(realm, script)
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
    void testExecuteJobScriptAndJobIdentifier() {
        assertNotNull(realm.save())
        TestJob testJob = createTestJobWithProcessingStep(AlignmentPass.build())
        testJob.log = log
        schedulerService.startingJobExecutionOnCurrentThread(testJob)
        try {
            String script = SCRIPT_CONTENT
            ProcessingOption processingOption = new ProcessingOption(
                    name: PbsOptionMergingService.PBS_PREFIX + JOB_IDENTFIER,
                    type: Realm.Cluster.DKFZ.toString(),
                    value: "{-l: {walltime: '00:01:00'}}",
                    comment: 'comment'
                    )
            assertNotNull(processingOption.save())
            // Send script to pbs
            String response = executionService.executeJob(realm, script, JOB_IDENTFIER)
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
    void testExecuteJobScriptAndJobIdentifierAndQsubParameter() {
        assertNotNull(realm.save())
        TestJob testJob = createTestJobWithProcessingStep(AlignmentPass.build())
        testJob.log = log
        schedulerService.startingJobExecutionOnCurrentThread(testJob)
        try {
            String script = SCRIPT_CONTENT
            ProcessingOption processingOption = new ProcessingOption(
                    name: PbsOptionMergingService.PBS_PREFIX + JOB_IDENTFIER,
                    type: Realm.Cluster.DKFZ.toString(),
                    value: "{-l: {walltime: '00:01:00'}}",
                    comment: 'comment'
                    )
            assertNotNull(processingOption.save())
            // Send script to pbs
            String response = executionService.executeJob(realm, script, JOB_IDENTFIER, QSUB_PARAMETERS)
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
    void testExecuteCommandCheckFrequently() {
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(SCRIPT_CONTENT)
        // File has to be executable
        cmdFile.setExecutable(true)
        // Construct pbs command
        String cmd = "qsub ${cmdFile.name}"
        String extractedPbsId = getPbsIdForExecutedCommand(realm, cmd)
        // Make new pbs command to verify whether pbs job still is running
        int time = 0
        while (time < 20) {
            cmd = "qstat ${extractedPbsId}"
            // Send verifying command with recent pbs id to pbs
            String extractedPbsId_qstat = getPbsIdForExecutedCommand(realm, cmd)
            // Assert if the two extracted pbs ids are equal
            assertEquals(extractedPbsId, extractedPbsId_qstat)
            sleep(1000)
            time++
        }
    }

    @Ignore("OTP-1106")
    @Test(expected = ProcessingException)
    void testExecuteCommandConnectionFailed() {
        realm.host = "GROUP"
        assertNotNull(realm.save())
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(SCRIPT_CONTENT)
        // File has to be executable
        cmdFile.setExecutable(true)
        // Construct pbs command
        String cmd = "qsub ${cmdFile.name}"
        // Send command to pbs
        String response = executionService.executeCommand(realm, cmd)
    }

    @Ignore("OTP-1106")
    @Test
    void testCheckRunningJob() {
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(SCRIPT_CONTENT)
        // File has to be executable
        cmdFile.setExecutable(true)
        // Construct pbs command
        String cmd = "qsub ${cmdFile.name}"
        String extractedPbsId = getPbsIdForExecutedCommand(realm, cmd)
        int time = 0
        while (time < 20) {
            executionService.checkRunningJob(extractedPbsId, realm)
            sleep(1000)
            time++
        }
    }

    @Ignore("OTP-1106")
    @Test
    void testCheckRunningJobConnectionFailed() {
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText(SCRIPT_CONTENT)
        // File has to be executable
        cmdFile.setExecutable(true)
        // Construct pbs command
        String cmd = "qsub ${cmdFile.name}"
        String extractedPbsId = getPbsIdForExecutedCommand(realm, cmd)
        realm.host = "GROUP"
        assertNotNull(realm.save())
        int time = 0
        while (time < 20) {
            assertTrue(executionService.checkRunningJob(extractedPbsId, realm))
            sleep(1000)
            time++
        }
    }

    /**
     * The method executes the given command on the cluster and returns the PBS ID.
     */
    String getPbsIdForExecutedCommand(Realm realm, String cmd) {
        String response = executionService.executeCommand(realm, cmd)
        // Extract pbs ids
        List<String> extractedPbsIds = executionService.extractPbsIds(response)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        assertEquals(1, extractedPbsIds.size())
        return extractedPbsIds.get(0)
    }
}
