package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*
import org.junit.*

class ProcessStatusServiceTests {

    ProcessStatusService processStatusService

    private static final String LOG_FILE_DIRECTORY = "/tmp/statusTest"
    private static final String LOG_FILE = "/tmp/statusTest/status.log"
    File dir
    File file

    @Before
    void setUp() {
        dir = new File(LOG_FILE_DIRECTORY)
        if (!dir.exists()) {
            assertTrue(dir.mkdirs())
        }
        file = new File(LOG_FILE)
        if (!file.exists()) {
            assertTrue(file.createNewFile())
        }
    }

    @After
    void tearDown() {
        file.setWritable(true)
        file.setReadable(true)
        dir.setWritable(true)
        assertTrue(file.delete())
        assertTrue(dir.delete())
    }

    @Test(expected = IllegalArgumentException)
    void testStatusLogFileNull() {
        processStatusService.statusLogFile(null)
    }

    @Test
    void testStatusLogFile() {
        assertEquals(LOG_FILE, processStatusService.statusLogFile(LOG_FILE_DIRECTORY))
    }

    @Test(expected = IllegalArgumentException)
    void testStatusSuccessfulLogFileNull() {
        processStatusService.statusSuccessful(null, "PreviousJob")
    }

    @Test(expected = IllegalArgumentException)
    void testStatusSuccessfulPreviousJobNull() {
        processStatusService.statusSuccessful(LOG_FILE, null)
    }

    @Test(expected = IllegalArgumentException)
    void testStatusSuccessfulNotReadable() {
        file.setReadable(false)
        processStatusService.statusSuccessful(LOG_FILE, "PreviousJob")
    }

    @Test
    void testStatusSuccessful() {
        file << "PreviousJob\n"
        assertTrue(processStatusService.statusSuccessful(LOG_FILE, "PreviousJob"))
    }

    @Test
    void testStatusSuccessfulJobNotInFile() {
        file << "WrongPreviousTestJob\n"
        assertFalse(processStatusService.statusSuccessful(LOG_FILE, "PreviousJob"))
    }
}
