package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*

import java.text.SimpleDateFormat
import javax.servlet.ServletContext
import org.apache.commons.io.FileUtils

import org.junit.*

import de.dkfz.tbi.otp.job.jobs.TestJob;

class ErrorLogServiceTests {

   /**
    * Dependency Injection of Error log service
    */
    def errorLogService

   /**
    * Dependency injection of grails Application
    */
    def grailsApplication

   /**
    * Dependency injection of Servlet Context
    */
   def servletContext

    @Before
    void setUp() {
    }

    @After
    void tearDown() {
    }

    @Test
    void testWriteStacktraceToFile() {
        // Create a job
        TestJob testJob = new TestJob()
        String jobClassName = testJob.class.name
        Exception e = new Exception("test message")
        errorLogService.writeStacktraceToFile(jobClassName, e)
        // To test if md5 is correctly taken as file name
        String md5Sum = (jobClassName + e.toString()).encodeAsMD5()
        // Try to get the file which should be created
        String fileName = md5Sum + ".xml"
        String fullPath = servletContext.getRealPath("")
        File exceptionStoringFile = new File(fullPath + "/", md5Sum + ".xml")
        // Test if it really is a file
        assertTrue(exceptionStoringFile.isFile())
        // Test if the file's path is the expected one
        assertEquals(fullPath + md5Sum + ".xml", exceptionStoringFile.path)
        if (exceptionStoringFile.exists()) {
            // To be absolutely sure that file has correct name
            assertEquals(md5Sum + ".xml", exceptionStoringFile.name)
            def root = new XmlParser().parse(exceptionStoringFile)
            def timestamps = root.timestamp.findAll{ it }
            timestamps.each {
                // Test whether date stored in file is really a date and as such being parsable as Date
                SimpleDateFormat sdfToDate = new SimpleDateFormat("E MMM d h:m:s Z yyyy")
                Date date = sdfToDate.parse(it.text())
                assertSame(new Date().class, date.class)
            }
            // Test if the root node has correct attributes
            assertTrue(root.@jobClass == "de.dkfz.tbi.otp.job.jobs.TestJob")
            assertTrue(root.@exceptionMessage == "test message")
        }
    }
}
