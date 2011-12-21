package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*
import org.junit.*

class PbsServiceTests {

    def pbsService

    @SuppressWarnings("EmptyMethod")
    void setUp() {
        // Setup logic here
    }

    @SuppressWarnings("EmptyMethod")
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testSendPbsJob() {
        println("testSendPbsJob")
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
        // Send command to pbs via helper class
        File responseFile = pbsService.sendPbsJob(cmd)
        // Extract pbs ids from temporary file
        List<String> extractedPbsIds = pbsService.extractPbsIds(responseFile)
        assertNotNull(extractedPbsIds)
        // Only one pbs id is set
        String extractedPbsId = extractedPbsIds.get(0)
        // Make new pbs command to verify whether pbs job still is running
        cmd = "qstat ${extractedPbsId}"
        // Send verifying commat with recent pbs id to pbs
        File checkFile = pbsService.sendPbsJob(cmd)
        extractedPbsIds = pbsService.extractPbsIds(checkFile)
        assertNotNull(extractedPbsIds)
        String extractedPbsId_qstat = extractedPbsIds.get(0)
        // Assert if the two extracted pbs ids are equal
        assertEquals(extractedPbsId, extractedPbsId_qstat)
    }

    @Test
    void testValidate() {
        println("testValidate")
        // Create temp file
        File cmdFile = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        cmdFile.setText("""#! /bin/bash
                            date
                            sleep 200
                            """)
        cmdFile.setExecutable(true)
        // Make executable file a pbs job
        String cmd = "qsub ${cmdFile.name}"
        File responseFile = pbsService.sendPbsJob(cmd)
        List<String> extractedPbsIds = pbsService.extractPbsIds(responseFile)
        assertNotNull(extractedPbsIds)
        // Get validation identifier
        Map<String, Boolean> validatedIds = pbsService.validate(extractedPbsIds)
        validatedIds.each { String key, Boolean value ->
            println("key: ${key}: ${value}")
            // Is job running?
            assertNotNull(key)
            assertTrue(value)
        }
    }
}
