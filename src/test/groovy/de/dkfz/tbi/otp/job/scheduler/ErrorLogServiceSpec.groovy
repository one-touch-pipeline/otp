/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.scheduler

import grails.testing.gorm.DataTest
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService

class ErrorLogServiceSpec extends Specification implements DataTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    ErrorLogService errorLogService

    File exceptionStoringFile
    File stacktraceFile

    final static String ARBITRARY_STACKTRACE_IDENTIFIER = "689f127e9492f1e242192288ea870f28"
    final static String ERROR_MESSAGE = "Exception"

    void setupData() {
        errorLogService = new ErrorLogService()
        errorLogService.configService = new TestConfigService()
        stacktraceFile = errorLogService.getStackTracesFile(ARBITRARY_STACKTRACE_IDENTIFIER)
        stacktraceFile.parentFile.mkdirs()
    }

    void cleanup() {
        new FileUtils().deleteQuietly(exceptionStoringFile)
        assert stacktraceFile.parentFile.deleteDir()
    }

    void "testLog"() {
        given:
        setupData()

        File testDirectory = tmpDir.newFolder("otp-test", "stacktraces")
        errorLogService.configService = Mock(TestConfigService) {
            getStackTracesDirectory() >> { new File(testDirectory.absolutePath) }
        }

        when:
        String md5SumCalculatedInMethod = errorLogService.log(new Exception(ERROR_MESSAGE))
        exceptionStoringFile = new File(testDirectory, md5SumCalculatedInMethod + ".xml")
        def contentOfFile = new XmlParser().parse(exceptionStoringFile)

        then:
        exceptionStoringFile.isFile()
        contentOfFile.timestamp.count { it } == 1
        // @ forces direct access, instead of going over the automatically generated getter
        contentOfFile.@exceptionMessage == ERROR_MESSAGE
    }

    void "testLoggedErrorNoFile"() {
        given:
        setupData()

        when:
        errorLogService.loggedError("/.|\test/..")

        then:
        RuntimeException e = thrown()
        e.message.contains("is not a file")
    }

    void "testLoggedErrorNoStackTraceContent"() {
        given:
        setupData()

        when:
        stacktraceFile.createNewFile()
        errorLogService.loggedError(ARBITRARY_STACKTRACE_IDENTIFIER)

        then:
        RuntimeException e = thrown()
        e.message.contains("The XML file could not be parsed")
    }

    void "testLoggedErrorWithNoXMLContent"() {
        given:
        setupData()

        when:
        stacktraceFile << "garbage"
        errorLogService.loggedError(ARBITRARY_STACKTRACE_IDENTIFIER)

        then:
        RuntimeException e = thrown()
        e.message.contains("The XML file could not be parsed")
    }

    void "testLoggedErrorWithContent"() {
        given:
        setupData()

        stacktraceFile << """
<stacktraceElement exceptionMessage='Testing'>
  <stacktrace>${ERROR_MESSAGE}</stacktrace>
  <timestamp>
        Thu Jul 18 11:24:14 CEST 2013
  </timestamp>
</stacktraceElement>
"""

        expect:
        ERROR_MESSAGE == errorLogService.loggedError(ARBITRARY_STACKTRACE_IDENTIFIER)
    }
}
