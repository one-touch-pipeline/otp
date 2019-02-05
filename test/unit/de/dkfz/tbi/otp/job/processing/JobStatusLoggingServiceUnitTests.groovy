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

package de.dkfz.tbi.otp.job.processing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.After
import org.junit.Test

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * Unit tests for the {@link JobStatusLoggingService}.
 */
@TestFor(JobStatusLoggingService)
@TestMixin(GrailsUnitTestMixin)
@Build([JobDefinition, JobExecutionPlan, Process, ProcessingOption, ProcessingStep, Realm])
class JobStatusLoggingServiceUnitTests {
    TestConfigService configService

    final static String LOGGING_ROOT_PATH = '/fakeRootPath'
    final static String EXPECTED_BASE_PATH = '/fakeRootPath/log/status'
    static String EXPECTED_LOGFILE_PATH

    final static Long ARBITRARY_ID = 23
    final static Long ARBITRARY_REALM_ID = 987
    final static Long ARBITRARY_PROCESS_ID = 12345
    final static String ARBITRARY_PBS_ID = '4711'

    static ProcessingStep createFakeProcessingStep() {
        return DomainFactory.createProcessingStep([
                id           : ARBITRARY_ID,
                process      : DomainFactory.createProcess([id: ARBITRARY_PROCESS_ID]),
                jobClass     : 'this.is.a.DummyJob',
        ])
    }

    Realm realm

    void setUp() {
        realm = DomainFactory.createRealm()
        configService = new TestConfigService([(OtpProperty.PATH_CLUSTER_LOGS_OTP): LOGGING_ROOT_PATH])
        service.configService = configService
        EXPECTED_LOGFILE_PATH = "/fakeRootPath/log/status/joblog_${ARBITRARY_PROCESS_ID}_${ARBITRARY_PBS_ID}_${realm.id}.log"
    }

    @After
    void tearDown() {
        configService.clean()
    }

    @Test
    void testLogFileBaseDirWhenProcessingStepIsNull() {
        shouldFail IllegalArgumentException, { service.logFileBaseDir(null) }
    }

    @Test
    void testLogFileLocationWhenRealmIsNull() {
        shouldFail IllegalArgumentException, { service.constructLogFileLocation(null, new ProcessingStep()) }
    }

    @Test
    void testLogFileLocationWhenProcessingStepIsNull() {
        shouldFail IllegalArgumentException, { service.constructLogFileLocation(new Realm(), null) }
    }

    @Test
    void testLogFileBaseDir() {
        ProcessingStep processingStep = createFakeProcessingStep()
        String actual = service.logFileBaseDir(processingStep)
        assert EXPECTED_BASE_PATH == actual
    }

    @Test
    void testLogFileLocationWhenClusterJobIdIsNotPassed() {
        ProcessingStep processingStep = createFakeProcessingStep()
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.configService = new TestConfigService()
        service.clusterJobManagerFactoryService.configService.processingOptionService = new ProcessingOptionService()
        def actual = service.constructLogFileLocation(realm, processingStep)
        assert "${EXPECTED_BASE_PATH}/joblog_${ARBITRARY_PROCESS_ID}_\$(echo \${PBS_JOBID} | cut -d. -f1)_${realm.id}.log" == actual
    }

    @Test
    void testLogFileLocationWhenClusterJobIdIsPassed() {
        ProcessingStep processingStep = createFakeProcessingStep()
        def actual = service.constructLogFileLocation(realm, processingStep, ARBITRARY_PBS_ID)
        assert EXPECTED_LOGFILE_PATH == actual
    }

    @Test
    void testConstructMessageWhenProcessingStepIsNull() {
        shouldFail IllegalArgumentException, { service.constructMessage(realm, null) }
    }

    @Test
    void testConstructMessageWhenClusterJobIdIsNotPassed() {
        ProcessingStep processingStep = createFakeProcessingStep()
        service.clusterJobManagerFactoryService = new ClusterJobManagerFactoryService()
        service.clusterJobManagerFactoryService.configService = new TestConfigService()
        service.clusterJobManagerFactoryService.configService.processingOptionService = new ProcessingOptionService()
        def actual = service.constructMessage(realm, processingStep)
        assert "${processingStep.jobExecutionPlan.name},DummyJob,${ARBITRARY_ID},\$(echo \${PBS_JOBID} | cut -d. -f1)" == actual
    }

    @Test
    void testConstructMessageWhenClusterJobIdIsPassed() {
        ProcessingStep processingStep = createFakeProcessingStep()
        def actual = service.constructMessage(null, processingStep, ARBITRARY_PBS_ID)
        assert "${processingStep.jobExecutionPlan.name},DummyJob,${ARBITRARY_ID},${ARBITRARY_PBS_ID}" == actual
    }
}
