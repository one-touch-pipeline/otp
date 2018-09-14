package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

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
