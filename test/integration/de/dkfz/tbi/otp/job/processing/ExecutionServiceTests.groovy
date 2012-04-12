package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class ExecutionServiceTests extends AbstractIntegrationTest {

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
    void testExecuteCommand() {
        println("testExecuteCommand")
        // Neither a command nor a script specified to be run remotely.
        shouldFail(ProcessingException) {
            executionService.executeCommand("DKFZ", null)
        }
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
        // No valid realm specified.
        shouldFail(ProcessingException) {
            executionService.executeCommand("", cmd)
        }
        // Send command to pbs
        String response = executionService.executeCommand("DKFZ", cmd)
        // Extract pbs ids
        List<String> extractedPbsIds = executionService.extractPbsIds(response)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        String extractedPbsId = extractedPbsIds.get(0)
        // Make new pbs command to verify whether pbs job still is running
        cmd = "qstat ${extractedPbsId}"
        // Send verifying command with recent pbs id to pbs
        String check = executionService.executeCommand("DKFZ", cmd)
        extractedPbsIds = executionService.extractPbsIds(check)
        assertNotNull(extractedPbsIds)
        String extractedPbsId_qstat = extractedPbsIds.get(0)
        // Assert if the two extracted pbs ids are equal
        assertEquals(extractedPbsId, extractedPbsId_qstat)
    }

    @Test
    void testExecuteJobScript() {
        println("testExecuteJobScript")
        // No file path specified.
        shouldFail(ProcessingException) {
            executionService.executeJobScript("DKFZ", null)
        }
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File file = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        file.setText("""#! /bin/bash
                       date
                       sleep 20
                       """)
        file.setExecutable(true)
        // No valid realm specified.
        shouldFail(ProcessingException) {
            executionService.executeJobScript("", file.absolutePath)
        }
        // Send file path to pbs
        String response = executionService.executeJobScript("DKFZ", file.absolutePath)
        // Extract pbs ids
        List<String> extractedPbsIds = executionService.extractPbsIds(response)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        String extractedPbsId = extractedPbsIds.get(0)
        // Make new pbs command to verify whether pbs job still is running
        String cmd = "qstat ${extractedPbsId}"
        // Send verifying command with recent pbs id to pbs
        String check = executionService.executeCommand("DKFZ", cmd)
        extractedPbsIds = executionService.extractPbsIds(check)
        assertNotNull(extractedPbsIds)
        String extractedPbsId_qstat = extractedPbsIds.get(0)
        // Assert if the two extracted pbs ids are equal
        assertEquals(extractedPbsId, extractedPbsId_qstat)
    }

    @Test
    void testExecuteJob() {
        println("testExecuteJob")
        // No job specified.
        shouldFail(ProcessingException) {
            executionService.executeJob("DKFZ", null)
        }
        String script = ("""#! /bin/bash
                       date
                       sleep 20
                       """)
        // No valid realm specified.
        shouldFail(ProcessingException) {
            executionService.executeJob("", script)
        }
        // Send script to pbs
        String response = executionService.executeJob("DKFZ", script)
        // Extract pbs ids
        List<String> extractedPbsIds = executionService.extractPbsIds(response)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        String extractedPbsId = extractedPbsIds.get(0)
        // Make new pbs command to verify whether pbs job still is running
        String cmd = "qstat ${extractedPbsId}"
        // Send verifying command with recent pbs id to pbs
        String check = executionService.executeCommand("DKFZ", cmd)
        extractedPbsIds = executionService.extractPbsIds(check)
        assertNotNull(extractedPbsIds)
        String extractedPbsId_qstat = extractedPbsIds.get(0)
        // Assert if the two extracted pbs ids are equal
        assertEquals(extractedPbsId, extractedPbsId_qstat)
    }
}
