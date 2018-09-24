package de.dkfz.tbi.otp.job.processing

import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired

import static org.junit.Assert.*

import org.apache.commons.io.FileUtils
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.acl.AclUtilService
import org.junit.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class ProcessServiceTests extends AbstractIntegrationTest {
    AclUtilService aclUtilService
    ErrorLogService errorLogService

    @Autowired
    ProcessService processService

    @Autowired
    LinkGenerator linkGenerator

    @Before
    void setUp() {
        createUserAndRoles()
    }

    @After
    void tearDown() {
    }

    /**
     * Tests that getProcess returns null in case of non existing Process
     */
    @Test
    void testGetProcessNonExisting() {
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(processService.getProcess(1))
        }
    }

    /**
     * Tests that an operator user has access to all processes
     */
    @Test
    void testGetProcessAsOperator() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        SpringSecurityUtils.doWithAuth("operator") {
            assertSame(process, processService.getProcess(process.id))
        }
    }

    /**
     * Tests that an admin user has access to all processes
     */
    @Test
    void testGetProcessAsAdmin() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        SpringSecurityUtils.doWithAuth("admin") {
            assertSame(process, processService.getProcess(process.id))
        }
    }

    /**
     * Tests that a user has no access to a process unless ACL is defined
     * on the JobExecutionPlan
     */
    @Test
    void testGetProcessAsUser() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getProcess(process.id)
            }
        }
    }

    /**
     * Tests security of getAllProcessingSteps:
     * user should only have access if ACL defined on JobExecutionPlan
     */
    @Test
    void testGetAllProcessingStepsSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(processService.getAllProcessingSteps(process).empty)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getAllProcessingSteps(process)
            }
        }
    }

    /**
     * Tests security of getNumberOfProcessingSteps:
     * user should only have access if ACL defined on JobExecutionPlan
     */
    @Test
    void testNumberOfProcessingStepsPermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(0, processService.getNumberOfProcessessingSteps(process))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getNumberOfProcessessingSteps(process)
            }
        }
    }

    /**
     * Tests that getProcessingStep returns null in case of non existing ProcessingStep
     */
    @Test
    void testGetProcessingStepNonExisting() {
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(processService.getProcessingStep(1))
        }
    }

    /**
     * Tests that an operator user has access to all processing steps
     */
    @Test
    void testGetProcessingStepAsOperator() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        SpringSecurityUtils.doWithAuth("operator") {
            assertSame(step, processService.getProcessingStep(step.id))
        }
    }

    /**
     * Tests that an admin user has access to all processing steps
     */
    @Test
    void testGetProcessingStepAsAdmin() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        SpringSecurityUtils.doWithAuth(ADMIN) {
            assertSame(step, processService.getProcessingStep(step.id))
        }
    }

    @Test
    void testGetProcessingStepPermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            processService.getProcessingStep(step.id)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getProcessingStep(step.id)
            }
        }
    }

    @Test
    void testGetAllUpdatesPermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(processService.getAllUpdates(step).empty)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getAllUpdates(step)
            }
        }
    }

    @Test
    void testNumberOfUpdatesPermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(0, processService.getNumberOfUpdates(step))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getNumberOfUpdates(step)
            }
        }
    }

    /**
     * Tests security of getLatestProcessingStep:
     * user should only have access if ACL defined on JobExecutionPlan
     */
    @Test
    void testGetLatestProcessingStepPermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(processService.getLatestProcessingStep(process))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getLatestProcessingStep(process)
            }
        }
    }

    /**
     * Tests the security of getState:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    @Test
    void testGetStatePermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(ExecutionState.CREATED, processService.getState(process))
            assertEquals(ExecutionState.CREATED, processService.getState(step))
        }
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getState(process)
            }
            shouldFail(AccessDeniedException) {
                processService.getState(step)
            }
        }
    }

    @Test
    void testGetErrorPermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(processService.getError(process))
            assertNull(processService.getError(step))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getError(process)
            }
            shouldFail(AccessDeniedException) {
                processService.getError(step)
            }
        }
    }

    /**
     * Tests the security of getLastUpdate:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    @Test
    void testGetLastUpdatePermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(update.date, processService.getLastUpdate(process))
            assertEquals(update.date, processService.getLastUpdate(step))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getLastUpdate(process)
            }
            shouldFail(AccessDeniedException) {
                processService.getLastUpdate(step)
            }
        }
    }

    @Test
    void testGetLatestProcessingStepUpdatePermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertSame(update, processService.getLatestProcessingStepUpdate(step))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getLatestProcessingStepUpdate(step)
            }
        }
    }

    @Test
    void testGetFirstUpdatePermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(update.date, processService.getFirstUpdate(step))
        }
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getFirstUpdate(step)
            }
        }
    }

    @Test
    void testGetProcessingStepDurationPermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(processService.getProcessingStepDuration(step))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.getProcessingStepDuration(step)
            }
        }
    }

    @Test
    void testProcessInformationPermission() {
        JobExecutionPlan plan = mockPlan()
        StartJobDefinition startJob = new StartJobDefinition(name: "StartJobTest", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save(flush: true))
        plan.startJob = startJob
        assertNotNull(plan.save(flush: true))
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            processService.processInformation(process)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.processInformation(process)
            }
        }
    }

    /**
     * Tests the security of restartProcessingStep:
     * requires write permission ACL on JobExecutionPlan
     */
    @Test
    void testRestartProcessingStepPermission() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            shouldFail(IncorrectProcessingException) {
                processService.restartProcessingStep(step)
            }
        }
        SpringSecurityUtils.doWithAuth(USER) {
            shouldFail(AccessDeniedException) {
                processService.restartProcessingStep(step)
            }
        }
    }

    @Test(expected = RuntimeException)
    void testGetProcessingErrorStackTraceIdNotFound() {
        processService.getProcessingErrorStackTrace(1)
    }

    @Test(expected = RuntimeException)
    void testGetProcessingErrorStackTraceNoStacktrace() {
        ProcessingError processingError = mockProcessingError()
        processingError.stackTraceIdentifier = null
        assertNotNull(processingError.save(flush: true))
        processService.getProcessingErrorStackTrace(processingError.id)
    }

    @Test(expected = RuntimeException)
    void testGetProcessingErrorStackTraceIdFoundAndPermissionsWrong() {
        ProcessingError processingError = mockProcessingError()
        SpringSecurityUtils.doWithAuth("testuser") {
            processService.getProcessingErrorStackTrace(processingError.id)
        }
    }

    @Test
    void testGetProcessingErrorStackTracePermissionIsRoleOperator() {
        ProcessingError processingError = mockProcessingError()
        File stacktraceFile = errorLogService.getStackTracesFile(processingError.stackTraceIdentifier)
        FileUtils.forceMkdir(stacktraceFile.parentFile)
        if (stacktraceFile.exists()) {
            assert stacktraceFile.delete()
        }
        assert stacktraceFile.createNewFile()
        stacktraceFile.deleteOnExit()
        stacktraceFile <<
                        """
<stacktraceElement exceptionMessage='Testing'>
    <stacktrace>
        Exception
  </stacktrace>
  <timestamp>
    Thu Jul 18 11:24:14 CEST 2013
  </timestamp>
</stacktraceElement>
"""
        SpringSecurityUtils.doWithAuth("operator") {
            String expectedMessage = errorLogService.loggedError(processingError.stackTraceIdentifier)
            String actualMessage = processService.getProcessingErrorStackTrace(processingError.id)
            assertEquals(expectedMessage, actualMessage)
        }
        stacktraceFile.delete()
    }

    @Test
    void testProcessUrl_shouldBeFine() {
        String serverUrl = linkGenerator.getServerBaseURL()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        String url = processService.processUrl(process)
        assert "${serverUrl}/processes/process/${process.id}" == url
    }



    private JobExecutionPlan mockPlan(String name = "test") {
        JobExecutionPlan plan = new JobExecutionPlan(name: name, planVersion: 0, enabled: true)
        assertNotNull(plan.save(flush: true))
        return plan
    }

    private Process mockProcess(JobExecutionPlan plan) {
        Process process = new Process(started: new Date(), startJobClass: "foo", jobExecutionPlan: plan)
        assertNotNull(process.save(flush: true))
        return process
    }

    private ProcessingStep mockProcessingStep(Process process, JobDefinition job) {
        ProcessingStep processingStep = new ProcessingStep(jobDefinition: job, process: process)
        assertNotNull(processingStep.save(flush: true))
        return processingStep
    }

    private ProcessingStepUpdate mockProcessingStepUpdate(ProcessingStep step, ExecutionState state = ExecutionState.CREATED, ProcessingStepUpdate previous = null) {
        ProcessingStepUpdate update = new ProcessingStepUpdate(previous: previous, state: state, date: new Date(), processingStep: step)
        assertNotNull(update.save(flush: true))
        return update
    }

    private ProcessingError mockProcessingError() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        update.state = ExecutionState.FAILURE
        assertNotNull(update.save(flush: true))

        ProcessingError processingError = new ProcessingError(
                        processingStepUpdate: update,
                        errorMessage: "errorMessage",
                        stackTraceIdentifier: "stackTraceIdentifier"
                        )
        assertNotNull(processingError.save(flush: true))
        return processingError
    }
}
