package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.TestCase
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ErrorLogServiceTests {

    ErrorLogService errorLogService
    GrailsApplication grailsApplication

    File exceptionStoringFile
    File stacktraceFile

    final static String ARBITRARY_STACKTRACE_IDENTIFIER = "689f127e9492f1e242192288ea870f28"
    final static String ERROR_MESSAGE = "Exception"

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

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
        tmpDir.create()
        File testDirectory = tmpDir.newFolder("/otp-test/stacktraces/")
        errorLogService.metaClass.getStackTracesDirectory = {
            return testDirectory.absolutePath
        }
        // To test whether calling log method produces error
        Exception e = new Exception(ERROR_MESSAGE)
        String md5SumCalculatedInMethod = errorLogService.log(e)
        exceptionStoringFile = new File(testDirectory, md5SumCalculatedInMethod + ".xml")
        // Test if the file exists
        assertTrue(exceptionStoringFile.isFile())
        //Test if the content of the file is correct
        def contentOfFile = new XmlParser().parse(exceptionStoringFile)
        def timestamps = contentOfFile.timestamp.findAll{ it }
        assertEquals(1, timestamps.size())
        assertTrue(contentOfFile.@exceptionMessage == ERROR_MESSAGE)
        TestCase.removeMetaClass(ErrorLogService, errorLogService)
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
