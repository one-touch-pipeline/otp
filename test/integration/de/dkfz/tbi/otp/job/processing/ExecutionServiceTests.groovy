package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.Realm.OperationType;

class ExecutionServiceTests extends AbstractIntegrationTest {

    def executionService
    def grailsApplication

    Realm realm

    @Before
    void setUp() {
        realm = new Realm(name: "DKFZ", env: "development", operationType: OperationType.DATA_MANAGEMENT, rootPath: "/", processingRootPath: "/test", programsRootPath: "/testPrograms", webHost: "http://test.me", host: "headnode", port: 22, unixUser: "unixUser", timeout: 100, pbsOptions: "")
    }

    @SuppressWarnings("EmptyMethod")
    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testExecuteCommand() {
        println("testExecuteCommand")
        assertNotNull(realm.save())
        // Neither a command nor a script specified to be run remotely.
        shouldFail(ProcessingException) {
            executionService.executeCommand(realm, null)
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
        String check = executionService.executeCommand(realm, cmd)
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
            executionService.executeJobScript(realm, null)
        }
        // Create temporary file to be executed on pbs. The file is stored on user's home.
        File file = File.createTempFile("test", ".tmp", new File(System.getProperty("user.home")))
        file.setText("""#! /bin/bash
                       date
                       sleep 20
                       """)
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
        String check = executionService.executeCommand(realm, cmd)
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
            executionService.executeJob(realm, null)
        }
        String script = ("""#! /bin/bash
                       date
                       sleep 20
                       """)
        // No valid realm specified.
        shouldFail(NullPointerException) {
            executionService.executeJob(null, script)
        }
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
        String check = executionService.executeCommand(realm, cmd)
        extractedPbsIds = executionService.extractPbsIds(check)
        assertNotNull(extractedPbsIds)
        String extractedPbsId_qstat = extractedPbsIds.get(0)
        // Assert if the two extracted pbs ids are equal
        assertEquals(extractedPbsId, extractedPbsId_qstat)
    }
}
