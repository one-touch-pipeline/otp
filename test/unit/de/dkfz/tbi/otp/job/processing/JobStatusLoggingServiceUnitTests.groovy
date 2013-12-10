package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 * Unit tests for the {@link JobStatusLoggingService}.
 *
 */
@TestFor(JobStatusLoggingService)
@TestMixin(GrailsUnitTestMixin)
@Mock([JobDefinition, JobExecutionPlan, Process, ProcessingStep, Realm])
class JobStatusLoggingServiceUnitTests extends GroovyTestCase {

    final static String LOGGING_ROOT_PATH = '/fakeRootPath'
    final static String EXPECTED_BASE_PATH = '/fakeRootPath/log/status'
    final static String EXPECTED_LOGFILE_PATH = '/fakeRootPath/log/status/joblog_12345.log'

    final static Long ARBITRARY_ID = 23
    final static Long ARBITRARY_PROCESS_ID = 12345
    final static String ARBITRARY_PBS_ID = '4711'

    private ProcessingStep createFakeProcessingStep() {
        ProcessingStep processingStep = new ProcessingStep([id: ARBITRARY_ID])
        processingStep.with {
            jobDefinition = new JobDefinition()
            jobDefinition.with {
                plan = new JobExecutionPlan([name: 'SomeWorkflowName'])
            }
            process = new Process([id: ARBITRARY_PROCESS_ID])
            jobClass = 'this.is.a.DummyJob'
        }
        processingStep
    }

    @Test
    void testLogFileBaseDirWhenRealmIsNull() {
        shouldFail IllegalArgumentException, { service.logFileBaseDir(null, new ProcessingStep()) }
    }

    @Test
    void testLogFileBaseDirWhenProcessingStepIsNull() {
        shouldFail IllegalArgumentException, { service.logFileBaseDir(new Realm(), null) }
    }

    @Test
    void testLogFileLocationWhenRealmIsNull() {
        shouldFail IllegalArgumentException, { service.logFileLocation(null, new ProcessingStep()) }
    }

    @Test
    void testLogFileLocationWhenProcessingStepIsNull() {
        shouldFail IllegalArgumentException, { service.logFileLocation(new Realm(), null) }
    }

    @Test
    void testLogFileBaseDir() {
        Realm realm = new Realm([loggingRootPath: LOGGING_ROOT_PATH])
        ProcessingStep processingStep = createFakeProcessingStep()
        def actual = service.logFileBaseDir(realm, processingStep)
        assert EXPECTED_BASE_PATH == actual
    }

    @Test
    void testLogFileLocation() {
        Realm realm = new Realm([loggingRootPath: LOGGING_ROOT_PATH])
        ProcessingStep processingStep = createFakeProcessingStep()
        def actual = service.logFileLocation(realm, processingStep)
        assert EXPECTED_LOGFILE_PATH == actual
    }

    @Test
    void testConstructMessageWhenProcessingStepIsNull() {
        shouldFail IllegalArgumentException, { service.constructMessage(null) }
    }

    @Test
    void testConstructMessageWhenPbsIdIsNotPassed() {
        ProcessingStep processingStep = createFakeProcessingStep()
        def actual = service.constructMessage(processingStep)
        assert "SomeWorkflowName,DummyJob,23,${service.SHELL_SNIPPET_GET_NUMERIC_PBS_ID}" == actual
    }

    @Test
    void testConstructMessageWhenPbsIdIsPassed() {
        ProcessingStep processingStep = createFakeProcessingStep()
        def actual = service.constructMessage(processingStep, ARBITRARY_PBS_ID)
        assert 'SomeWorkflowName,DummyJob,23,4711' == actual
    }
}
