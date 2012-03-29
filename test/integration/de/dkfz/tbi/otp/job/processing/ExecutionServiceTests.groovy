package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class ExecutionServiceTests {

    def executionService
    def grailsApplication

    @SuppressWarnings("EmptyMethod")
    @Before
    void setUp() {
        // Setup logic here
    }

    @SuppressWarnings("EmptyMethod")
    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testExecuteRemoteJobAsCommand() {
        println("testExecuteRemoteJobAsCommand")
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText("""#! /bin/bash
                            date
                            sleep 200
                            """)
        // File has to be executable
        cmdFile.setExecutable(true)
        // Construct pbs command
        String cmd = "qsub ${cmdFile.name}"
        String host = (grailsApplication.config.otp.pbs.ssh.host).toString()
        Integer timeout = (grailsApplication.config.otp.pbs.ssh.timeout) as Integer
        // Send command to pbs
        String response = executionService.executeRemoteJob(host, 22, timeout, cmd, null)
        // Extract pbs ids
        List<String> extractedPbsIds = executionService.extractPbsIds(response)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        String extractedPbsId = extractedPbsIds.get(0)
        // Make new pbs command to verify whether pbs job still is running
        cmd = "qstat ${extractedPbsId}"
        // Send verifying command with recent pbs id to pbs
        String check = executionService.executeRemoteJob(host, 22, timeout, cmd, null)
        extractedPbsIds = executionService.extractPbsIds(check)
        assertNotNull(extractedPbsIds)
        String extractedPbsId_qstat = extractedPbsIds.get(0)
        // Assert if the two extracted pbs ids are equal
        assertEquals(extractedPbsId, extractedPbsId_qstat)
    }

    @Test
    void testExecuteRemoteJobAsFile() {
        println("testExecuteRemoteJobAsFile")
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File file = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        file.setText("""#! /bin/bash
                       date
                       sleep 20
                       """)
        file.setExecutable(true)
        String host = (grailsApplication.config.otp.pbs.ssh.host).toString()
        Integer timeout = (grailsApplication.config.otp.pbs.ssh.timeout) as Integer
        // Send command to pbs
        String response = executionService.executeRemoteJob(host, 22, timeout, null, file)
        // Extract pbs ids
        List<String> extractedPbsIds = executionService.extractPbsIds(response)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        String extractedPbsId = extractedPbsIds.get(0)
        // Make new pbs command to verify whether pbs job still is running
        String cmd = "qstat ${extractedPbsId}"
        // Send verifying command with recent pbs id to pbs
        String check = executionService.executeRemoteJob(host, 22, timeout, cmd, null)
        extractedPbsIds = executionService.extractPbsIds(check)
        assertNotNull(extractedPbsIds)
        String extractedPbsId_qstat = extractedPbsIds.get(0)
        // Assert if the two extracted pbs ids are equal
        assertEquals(extractedPbsId, extractedPbsId_qstat)
    }
}
