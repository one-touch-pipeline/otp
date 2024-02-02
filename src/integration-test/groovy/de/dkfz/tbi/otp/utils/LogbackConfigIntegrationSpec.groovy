/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.utils

import ch.qos.logback.classic.*
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.Environment
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Rollback
@Integration
class LogbackConfigIntegrationSpec extends Specification {

    @Shared
    ByteArrayOutputStream mockedStdoutStream

    TestConfigService configService

    final static String LOG_MESSAGE = "Unique log message: "
    final static String JOB_ID = UUID.randomUUID()
    final static String POSTFIX = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    Path logFileOTP
    Path logFileStartjob
    Path logFileJob

    LoggerContext lc = (LoggerContext) LoggerFactory.ILoggerFactory
    Logger jobsLogger = lc.getLogger("de.dkfz.tbi.otp.job.jobs")
    Logger otpLogger = lc.getLogger("de.dkfz.tbi.otp")

    Appender<ILoggingEvent> jobsAppender = Spy(jobsLogger.getAppender("JOBS"))
    Appender<ILoggingEvent> startjobsAppender = Spy(jobsLogger.getAppender("STARTJOBS"))

    void setupSpec() {
        // Redirect System.out to buffer
        mockedStdoutStream = new ByteArrayOutputStream()
        System.out = new PrintStream(mockedStdoutStream)
    }

    void setup() {
        mockedStdoutStream.reset()

        jobsLogger.level = Level.INFO

        // OTP log files are created in the current working directory
        logFileOTP = Paths.get("logs/OTP-${POSTFIX}.log")

        // fetch the jobs log file paths, available in configService
        Path jobsLogDirectoryPath = configService.jobLogDirectory.toPath()
        logFileJob = jobsLogDirectoryPath.resolve("${JOB_ID}.log")
        logFileStartjob = jobsLogDirectoryPath.resolve("startjobs/${POSTFIX}.log")
    }

    @Unroll
    void "logback should write messages to stdout in development mode, but not in production mode"() {
        given:
        GroovyMock(Environment, global: true)
        Environment.isDevelopmentEnvironmentAvailable() >> isDevMode

        when:
        String msg = logText + UUID.randomUUID()
        otpLogger.info(msg)

        then:
        jobsAppender.started
        startjobsAppender.started

        // check log message in std out depending on the mode
        inStdout == new String(mockedStdoutStream.toByteArray()).contains(msg)

        // log message should always appear in OTP log file
        logFileOTP.text.contains(msg)

        // log inFile for startjob should be created
        Files.exists(logFileStartjob)

        where:
        isDevMode | logText                     || inStdout
        true      | "log in development mode: " || true
        false     | "log in production mode: "  || false
    }

    void "logback should write startjobs log message to file, but not jobs log"() {
        given:
        GroovyMock(Environment, global: true)
        Environment.isDevelopmentEnvironmentAvailable() >> isDevMode

        // have to manually modify the additive since logback.xml is already loaded before tests started
        jobsLogger.additive = Environment.isDevelopmentEnvironmentAvailable()

        MDC.clear()

        when:
        String msg = LOG_MESSAGE + UUID.randomUUID()
        jobsLogger.error(msg)

        then:
        jobsAppender.started
        startjobsAppender.started

        // no output in the inStdout
        inStdout == new String(mockedStdoutStream.toByteArray()).contains(msg)

        // log message should be in the OTP log
        inFile == logFileOTP.text.contains(msg)

        // startjob log exists and log message should be written into this file
        Files.exists(logFileStartjob)
        logFileStartjob.text.contains(msg)

        // no job log inFile should be created for this job
        !Files.exists(logFileJob)

        where:
        isDevMode || inStdout | inFile
        true      || true     | true
        false     || false    | false
    }

    @Unroll
    void "logback should write jobs log message to files, but no startjobs log written"() {
        given:
        GroovyMock(Environment, global: true)
        Environment.isDevelopmentEnvironmentAvailable() >> isDevMode

        // have to manually modify the additive since logback.xml is already loaded before tests started
        jobsLogger.additive = Environment.isDevelopmentEnvironmentAvailable()

        MDC.clear()
        MDC.put("PROCESS_AND_JOB_ID", JOB_ID)

        when:
        String msg = LOG_MESSAGE + UUID.randomUUID()
        jobsLogger.error(msg)

        then:
        jobsAppender.started
        startjobsAppender.started

        // no output in the inStdout
        inStdout == new String(mockedStdoutStream.toByteArray()).contains(msg)

        // log message should be in the OTP log
        inFile == logFileOTP.text.contains(msg)

        // startjob log file exists but no log should be written into this file
        Files.exists(logFileStartjob)
        !logFileStartjob.text.contains(msg)

        // a new job log file should be created and log message should be written into this file
        Files.exists(logFileJob)
        logFileJob.text.contains(msg)

        where:
        isDevMode || inStdout | inFile
        true      || true     | true
        false     || false    | false
    }
}
