package de.dkfz.tbi.otp.job.scheduler

import static org.junit.Assert.*

import java.text.SimpleDateFormat

import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.junit.*

class ErrorLogServiceTests {

    ErrorLogService errorLogService
    GrailsApplication grailsApplication

    File exceptionStoringFile
    File stacktraceFile

    final static String ARBITRARY_STACKTRACE_IDENTIFIER = "689f127e9492f1e242192288ea870f28"
    final static String ERROR_MESSAGE = "Exception"

    @Before
    void setUp() {
        stacktraceFile = errorLogService.getStackTracesFile(ARBITRARY_STACKTRACE_IDENTIFIER)
    }

    @After
    void tearDown() {
        new FileUtils().deleteQuietly(exceptionStoringFile)
        stacktraceFile.delete()
    }

    @Test
    void testLog() {
        Exception e = new Exception(ERROR_MESSAGE)
        String fullPath = "/tmp/otp/stacktraces/"
        // To test whether calling log method produces error
        errorLogService.log(e)
        // To test if md5 is correctly taken as file name
        String md5Sum = (e.toString()).encodeAsMD5()
        String fileName = md5Sum + ".xml"
        // Try to get the file which should be created
        exceptionStoringFile = new File(fullPath, md5Sum + ".xml")
        // Create and store new xml file through service
        errorLogService.contentToXml(e, exceptionStoringFile)
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
            SimpleDateFormat sdfToDate = new SimpleDateFormat("E MMM d h:m:s Z yyyy", Locale.ENGLISH)
            Date date = sdfToDate.parse(it.text())
            assertSame(new Date().class, date.class)
        }
        // Number of timestamps in file
        assertSame(2, timestamps.size())
        // Test if the root node has correct attributes
        assertTrue(root.@exceptionMessage == ERROR_MESSAGE)
    }

    @Test(expected = RuntimeException)
    void testLoggedErrorNoFile() {
        errorLogService.loggedError("/.|\test/..")
    }

    @Test(expected = RuntimeException)
    void testLoggedErrorNoStackTraceContent() {
        stacktraceFile.createNewFile()
        errorLogService.loggedError(ARBITRARY_STACKTRACE_IDENTIFIER)
    }

    @Test(expected = RuntimeException)
    void testLoggedErrorWithNoXMLContent() {
        stacktraceFile << ERROR_MESSAGE
        errorLogService.loggedError(ARBITRARY_STACKTRACE_IDENTIFIER)
    }

    @Test
    void testLoggedErrorWithContent() {
        stacktraceFile <<
                        """
<stacktraceElement exceptionMessage='Testing'>
  <stacktrace>${ERROR_MESSAGE}</stacktrace>
  <timestamp>
        Thu Jul 18 11:24:14 CEST 2013
  </timestamp>
</stacktraceElement>
"""
        assertEquals(ERROR_MESSAGE, errorLogService.loggedError(ARBITRARY_STACKTRACE_IDENTIFIER))
    }
}
