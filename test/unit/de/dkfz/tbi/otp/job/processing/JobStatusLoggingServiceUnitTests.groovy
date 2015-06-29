package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

/**
 * Unit tests for the {@link JobStatusLoggingService}.
 *
 */
@TestFor(JobStatusLoggingService)
@TestMixin(GrailsUnitTestMixin)
@Build([JobDefinition, JobExecutionPlan, Process, ProcessingStep, Realm])
class JobStatusLoggingServiceUnitTests {

    final static String LOGGING_ROOT_PATH = '/fakeRootPath'
    final static String EXPECTED_BASE_PATH = '/fakeRootPath/log/status'
    final static String EXPECTED_LOGFILE_PATH = "/fakeRootPath/log/status/joblog_${ARBITRARY_PROCESS_ID}_${ARBITRARY_REALM_ID}.log"

    final static Long ARBITRARY_ID = 23
    final static Long ARBITRARY_REALM_ID = 987
    final static Long ARBITRARY_PROCESS_ID = 12345
    final static String ARBITRARY_PBS_ID = '4711'

    static ProcessingStep createFakeProcessingStep() {
        return ProcessingStep.build([
                id           : ARBITRARY_ID,
                jobDefinition: JobDefinition.build(plan: JobExecutionPlan.build([name: 'SomeWorkflowName'])),
                process      : Process.build([id: ARBITRARY_PROCESS_ID]),
                jobClass     : 'this.is.a.DummyJob',
        ])
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
        Realm realm = Realm.build([id: ARBITRARY_REALM_ID, loggingRootPath: LOGGING_ROOT_PATH])
        ProcessingStep processingStep = createFakeProcessingStep()
        def actual = service.logFileBaseDir(realm, processingStep)
        assert EXPECTED_BASE_PATH == actual
    }

    @Test
    void testLogFileLocation() {
        Realm realm = Realm.build([id: ARBITRARY_REALM_ID, loggingRootPath: LOGGING_ROOT_PATH])
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
        assert "SomeWorkflowName,DummyJob,${ARBITRARY_ID},${service.SHELL_SNIPPET_GET_NUMERIC_PBS_ID}" == actual
    }

    @Test
    void testConstructMessageWhenPbsIdIsPassed() {
        ProcessingStep processingStep = createFakeProcessingStep()
        def actual = service.constructMessage(processingStep, ARBITRARY_PBS_ID)
        assert "SomeWorkflowName,DummyJob,${ARBITRARY_ID},${ARBITRARY_PBS_ID}" == actual
    }
}
