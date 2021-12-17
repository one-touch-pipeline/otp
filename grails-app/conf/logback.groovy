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

import ch.qos.logback.classic.joran.JoranConfigurator
import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')

        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%25.25t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
}

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir != null) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}

appender("OTP", RollingFileAppender) {
    append = true
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')
        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%25.25t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "logs/OTP-%d{yyyy-MM-dd}.log"
    }
}

appender("WAIT_TO_FILE_SYSTEM", RollingFileAppender) {
    append = true
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')
        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%25.25t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "logs/WAIT_TO_FILE_SYSTEM-%d{yyyy-MM-dd}.log"
        maxHistory = 7
        totalSizeCap = '1GB'
    }
}

List<String> appenderToUse = ['OTP']
if (Environment.current != Environment.PRODUCTION ){ //if not production, log also to stdout
    appenderToUse <<'STDOUT'
}

Properties otpProperties = ConfigService.parsePropertiesFile()
String jobLogDir = otpProperties.getProperty(OtpProperty.PATH_JOB_LOGS.key) ?: OtpProperty.PATH_JOB_LOGS.defaultValue

// this configuration creates a separate log file for each job execution (= processing step)
// it requires that the logging context is set, which is done in the SchedulerService
// as of version 1.2.3, the logback groovy config DSL doesn't support SiftingAppender,
// so we define this part of the config as XML
// see https://jira.qos.ch/browse/LOGBACK-957 and https://logback.qos.ch/manual/groovy.html#sift
JoranConfigurator configurator = new JoranConfigurator()
configurator.context = context
String jobLogConfig = """\
<configuration>
    <appender name="JOBS" class="ch.qos.logback.classic.sift.SiftingAppender">
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <evaluator class="ch.qos.logback.classic.boolex.GEventEvaluator">
                <expression>
                    e.mdc?.containsKey("PROCESS_AND_JOB_ID")
                </expression>
            </evaluator>
            <onMismatch>DENY</onMismatch>
            <onMatch>ACCEPT</onMatch>
        </filter>
        <discriminator class="ch.qos.logback.classic.sift.MDCBasedDiscriminator">
            <key>PROCESS_AND_JOB_ID</key>
            <defaultValue>THIS_FILE_SHOULD_NOT_EXIST</defaultValue><!--this shouldn't happen but defaultValue is required-->
        </discriminator>
        <sift>
            <appender name="JOB-\${PROCESS_AND_JOB_ID}" class="ch.qos.logback.core.FileAppender">
                <file>${jobLogDir}${File.separator}\${PROCESS_AND_JOB_ID}.log</file>
                <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
                    <layout class="de.dkfz.tbi.otp.utils.logging.ContentOnlyHtmlLayout">
                        <pattern>%date%level%logger{0}%msg</pattern>
                    </layout>
                </encoder>
            </appender>
        </sift>
    </appender>
    <appender name="STARTJOBS" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
            <evaluator class="ch.qos.logback.classic.boolex.GEventEvaluator">
                <expression>
                    e.mdc?.containsKey("PROCESS_AND_JOB_ID")
                </expression>
            </evaluator>
            <onMismatch>ACCEPT</onMismatch>
            <onMatch>DENY</onMatch>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- daily rollover, keeping seven days of logs-->
            <fileNamePattern>${jobLogDir}${File.separator}startjobs${File.separator}%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%date %level %-40.40logger{39} : %msg%n</pattern>
        </encoder>
    </appender>
    <logger name="de.dkfz.tbi.otp.job.jobs" level="DEBUG" additivity="${Environment.current.name == "WORKFLOW_TEST" ? "true":"false"}">
        <appender-ref ref="JOBS" />
        <appender-ref ref="STARTJOBS" />
    </logger>
</configuration>
"""
configurator.doConfigure(new ByteArrayInputStream(jobLogConfig.getBytes(StandardCharsets.UTF_8)))

logger("de.dkfz.tbi.otp.infrastructure.FileService.WAITING", DEBUG, ['WAIT_TO_FILE_SYSTEM'], false)
logger("de.dkfz.tbi.otp", DEBUG, appenderToUse, false)
logger("seedme", DEBUG, appenderToUse, false)
logger("liquibase", INFO, appenderToUse, false)
logger("grails.plugin.databasemigration", INFO, appenderToUse, false)
logger("org.hibernate.SQL", ERROR, appenderToUse, false)

logger("de.dkfz.roddy.execution.jobs.cluster", DEBUG, appenderToUse, false)

root(ERROR, appenderToUse)
