package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*

import java.text.SimpleDateFormat
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

   File exceptionStoringFile

    @SuppressWarnings("EmptyMethod")
    @Before
    void setUp() {
    }

    @After
    void tearDown() {
        new FileUtils().deleteQuietly(exceptionStoringFile)
    }

    @Test
    void testLog() {
        // Create a job
        TestJob testJob = new TestJob()
        Exception e = new Exception("test message")
        String path = "/target/stacktraces/testing/"
        String fullPath = servletContext.getRealPath(path)
        // To test whether calling log method produces error
        errorLogService.log(e)
        // To test if md5 is correctly taken as file name
        String md5Sum = (e.toString()).encodeAsMD5()
        String fileName = md5Sum + ".xml"
        // Try to get the file which should be created
        exceptionStoringFile = new File(fullPath, md5Sum + ".xml")
        // Create and store new xml file through service
        errorLogService.contentToXml(e, fileName, fullPath)
        // Test if it really is a file
        assertTrue(exceptionStoringFile.isFile())
        // Test if the file's path is the expected one
        assertEquals(fullPath + fileName , exceptionStoringFile.path)
        // Modify the xml file
        errorLogService.addToXml(exceptionStoringFile)
        def root = new XmlParser().parse(exceptionStoringFile)
        def timestamps = root.timestamp.findAll{ it }
        timestamps.each {
            // Test whether date stored in file is really a date and as such being parsable as Date
            SimpleDateFormat sdfToDate = new SimpleDateFormat("E MMM d h:m:s Z yyyy")
            Date date = sdfToDate.parse(it.text())
            assertSame(new Date().class, date.class)
        }
        // Number of timestamps in file
        assertSame(2, timestamps.size())
        // Test if the root node has correct attributes
        assertTrue(root.@exceptionMessage == "test message")
    }
}
