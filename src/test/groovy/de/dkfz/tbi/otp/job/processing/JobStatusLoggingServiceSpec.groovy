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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.*

class JobStatusLoggingServiceSpec extends Specification implements ServiceUnitTest<JobStatusLoggingService>, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                JobDefinition,
                JobExecutionPlan,
                Process,
                ProcessingOption,
                ProcessingStep,
        ]
    }

    TestConfigService configService

    final static String LOGGING_ROOT_PATH = '/fakeRootPath'
    final static String EXPECTED_BASE_PATH = '/fakeRootPath/log/status'

    final static Long ARBITRARY_ID = 23
    final static Long ARBITRARY_PROCESS_ID = 12345
    final static String ARBITRARY_PBS_ID = '4711'

    static ProcessingStep createFakeProcessingStep() {
        return DomainFactory.createProcessingStep([
                id           : ARBITRARY_ID,
                process      : DomainFactory.createProcess([id: ARBITRARY_PROCESS_ID]),
                jobClass     : 'this.is.a.DummyJob',
        ])
    }

    String expectedLogFilePath

    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_CLUSTER_LOGS_OTP): LOGGING_ROOT_PATH])
        service.configService = configService
        expectedLogFilePath = "/fakeRootPath/log/status/joblog_${ARBITRARY_PROCESS_ID}_${ARBITRARY_PBS_ID}.log"
    }

    void cleanup() {
        configService.clean()
    }

    void "test logFileBaseDir, when processing step is null"() {
        when:
        service.logFileBaseDir(null)

        then:
        thrown(IllegalArgumentException)
    }

    void "test constructLogFileLocation, when processing step is null"() {
        when:
        service.constructLogFileLocation(null)

        then:
        thrown(IllegalArgumentException)
    }

    void "test logFileBaseDir"() {
        given:
        ProcessingStep processingStep = createFakeProcessingStep()

        expect:
        EXPECTED_BASE_PATH == service.logFileBaseDir(processingStep)
    }

    void "test constructLogFileLocation, when cluster job ID is not passed"() {
        given:
        ProcessingStep processingStep = createFakeProcessingStep()
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.configService = Mock(ConfigService) {
            getJobScheduler() >> JobScheduler.PBS
        }

        expect:
        service.constructLogFileLocation(processingStep) ==
                "${EXPECTED_BASE_PATH}/joblog_${ARBITRARY_PROCESS_ID}_\$(echo \${PBS_JOBID} | cut -d. -f1).log"
    }

    void "test constructLogFileLocation, when cluster job ID is passed"() {
        given:
        ProcessingStep processingStep = createFakeProcessingStep()

        expect:
        expectedLogFilePath == service.constructLogFileLocation(processingStep, ARBITRARY_PBS_ID)
    }

    void "test constructMessage, when processing step is null"() {
        when:
        service.constructMessage(null)

        then:
        thrown(IllegalArgumentException)
    }

    void "test constructMessage, when cluster job ID is not passed"() {
        given:
        ProcessingStep processingStep = createFakeProcessingStep()
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.configService = Mock(ConfigService) {
            getJobScheduler() >> JobScheduler.PBS
        }

        expect:
        service.constructMessage(processingStep) ==
                "${processingStep.jobExecutionPlan.name},DummyJob,${ARBITRARY_ID},\$(echo \${PBS_JOBID} | cut -d. -f1)"
    }

    void "test constructMessage, when cluster job ID is passed"() {
        given:
        ProcessingStep processingStep = createFakeProcessingStep()

        expect:
        service.constructMessage(processingStep, ARBITRARY_PBS_ID) ==
                "${processingStep.jobExecutionPlan.name},DummyJob,${ARBITRARY_ID},${ARBITRARY_PBS_ID}"
    }
}
