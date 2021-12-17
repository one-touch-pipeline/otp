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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.acl.AclUtilService
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.web.mapping.LinkGenerator
import org.apache.commons.io.FileUtils
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.UserAndRoles

import static org.junit.Assert.*

@Deprecated
@Rollback
@Integration
class ProcessServiceIntegrationTests implements UserAndRoles {

    AclUtilService aclUtilService
    ErrorLogService errorLogService

    @Autowired
    ProcessService processService

    @Autowired
    LinkGenerator linkGenerator

    void setupData() {
        createUserAndRoles()
    }

    /**
     * Tests that getProcess returns null in case of non existing Process
     */
    @Test
    void testGetProcessNonExisting() {
        setupData()
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(processService.getProcess(1))
        }
    }

    /**
     * Tests that an operator user has access to all processes
     */
    @Test
    void testGetProcessAsOperator() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertSame(process, processService.getProcess(process.id))
        }
    }

    /**
     * Tests that an admin user has access to all processes
     */
    @Test
    void testGetProcessAsAdmin() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        SpringSecurityUtils.doWithAuth(ADMIN) {
            assertSame(process, processService.getProcess(process.id))
        }
    }

    /**
     * Tests that a user has no access to a process unless ACL is defined
     * on the JobExecutionPlan
     */
    @Test
    void testGetProcessAsUser() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(processService.getAllProcessingSteps(process).empty)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(0, processService.getNumberOfProcessingSteps(process))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                processService.getNumberOfProcessingSteps(process)
            }
        }
    }

    /**
     * Tests that getProcessingStep returns null in case of non existing ProcessingStep
     */
    @Test
    void testGetProcessingStepNonExisting() {
        setupData()
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(processService.getProcessingStep(1))
        }
    }

    /**
     * Tests that an operator user has access to all processing steps
     */
    @Test
    void testGetProcessingStepAsOperator() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertSame(step, processService.getProcessingStep(step.id))
        }
    }

    /**
     * Tests that an admin user has access to all processing steps
     */
    @Test
    void testGetProcessingStepAsAdmin() {
        setupData()
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
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            processService.getProcessingStep(step.id)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                processService.getProcessingStep(step.id)
            }
        }
    }

    @Test
    void testGetAllUpdatesPermission() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertTrue(processService.getAllUpdates(step).empty)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                processService.getAllUpdates(step)
            }
        }
    }

    @Test
    void testNumberOfUpdatesPermission() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(0, processService.getNumberOfUpdates(step))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertNull(processService.getLatestProcessingStep(process))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                processService.getLatestProcessingStep(process)
            }
        }
    }

    @Test
    void testGetLatestProcessingStep() {
        setupData()
        Process process = DomainFactory.createProcess()

        ProcessingStep processingStep = DomainFactory.createProcessingStep([
                process: process,
        ])
        RestartedProcessingStep restartedProcessingStep = DomainFactory.createRestartedProcessingStep([
                original: processingStep,
        ])

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            processService.getLatestProcessingStep(process) == restartedProcessingStep
        }
    }

    /**
     * Tests the security of getState:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    @Test
    void testGetStatePermission() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(ExecutionState.CREATED, processService.getState(process))
            assertEquals(ExecutionState.CREATED, processService.getState(step))
        }
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                processService.getState(process)
            }
            TestCase.shouldFail(AccessDeniedException) {
                processService.getState(step)
            }
        }
    }

    @Test
    void testGetErrorPermission() {
        setupData()
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
            TestCase.shouldFail(AccessDeniedException) {
                processService.getError(process)
            }
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
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
            TestCase.shouldFail(AccessDeniedException) {
                processService.getLastUpdate(process)
            }
            TestCase.shouldFail(AccessDeniedException) {
                processService.getLastUpdate(step)
            }
        }
    }

    @Test
    void testGetLatestProcessingStepUpdatePermission() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertSame(update, processService.getLatestProcessingStepUpdate(step))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                processService.getLatestProcessingStepUpdate(step)
            }
        }
    }

    @Test
    void testGetFirstUpdatePermission() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            assertEquals(update.date, processService.getFirstUpdate(step))
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                processService.getFirstUpdate(step)
            }
        }
    }

    @Test
    void testProcessInformationPermission() {
        setupData()
        JobExecutionPlan plan = mockPlan()
        StartJobDefinition startJob = new StartJobDefinition(name: "StartJobTest", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save(flush: true))
        plan.startJob = startJob
        assertNotNull(plan.save(flush: true))
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            processService.processInformation(process)
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
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
        setupData()
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        mockProcessingStepUpdate(step)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            TestCase.shouldFail(IncorrectProcessingException) {
                processService.restartProcessingStep(step)
            }
        }
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                processService.restartProcessingStep(step)
            }
        }
    }

    @Test(expected = RuntimeException)
    void testGetProcessingErrorStackTraceIdNotFound() {
        setupData()
        processService.getProcessingErrorStackTrace(1)
    }

    @Test(expected = RuntimeException)
    void testGetProcessingErrorStackTraceNoStacktrace() {
        setupData()
        ProcessingError processingError = mockProcessingError()
        processingError.stackTraceIdentifier = null
        assertNotNull(processingError.save(flush: true))
        processService.getProcessingErrorStackTrace(processingError.id)
    }

    @Test(expected = RuntimeException)
    void testGetProcessingErrorStackTraceIdFoundAndPermissionsWrong() {
        setupData()
        ProcessingError processingError = mockProcessingError()
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            processService.getProcessingErrorStackTrace(processingError.id)
        }
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    @Test
    void testGetProcessingErrorStackTracePermissionIsRoleOperator() {
        setupData()
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
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            String expectedMessage = errorLogService.loggedError(processingError.stackTraceIdentifier)
            String actualMessage = processService.getProcessingErrorStackTrace(processingError.id)
            assertEquals(expectedMessage, actualMessage)
        }
        stacktraceFile.delete()
    }

    @Test
    void testProcessUrl_shouldBeFine() {
        setupData()
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

    /**
     * Creates a JobDefinition for the testJob.
     * @param name Name of the JobDefinition
     * @param jep The JobExecutionPlan this JobDefinition will belong to
     * @param previous The previous Job Execution plan (optional)
     * @return Created JobDefinition
     * @deprecated this was copied here to be able to delete AbstractIntegrationTest. Don't use it, refactor it.
     */
    @Deprecated
    private JobDefinition createTestJob(String name, JobExecutionPlan jep, JobDefinition previous = null) {
        JobDefinition jobDefinition = new JobDefinition(name: name, bean: "testJob", plan: jep, previous: previous)
        assertNotNull(jobDefinition.save(flush: true))
        ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType test2 = new ParameterType(name: "test2", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType input = new ParameterType(name: "input", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        ParameterType input2 = new ParameterType(name: "input2", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        assertNotNull(test.save(flush: true))
        assertNotNull(test2.save(flush: true))
        assertNotNull(input.save(flush: true))
        assertNotNull(input2.save(flush: true))
        return jobDefinition
    }
}
