package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*

import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.grails.plugins.springsecurity.service.acl.AclUtilService
import org.junit.*
import org.springframework.security.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.scheduler.ErrorLogService
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

class ProcessServiceTests extends AbstractIntegrationTest {
    ProcessService processService
    AclUtilService aclUtilService
    ErrorLogService errorLogService
    GrailsApplication grailsApplication

    @Before
    void setUp() {
        // Setup logic here
        createUserAndRoles()
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    /**
     * Tests that getProcess returns null in case of non existing Process
     */
    @Test
    void testGetProcessNonExisting() {
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(processService.getProcess(1))
        }
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(processService.getProcess(1))
        }
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(processService.getProcess(1))
        }
    }

    /**
     * Tests that an operator user has access to all processes
     */
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
    void testGetProcessAsUser() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        // without ACL it should fail
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getProcess(process.id)
            }
        }
        // now grant the user access
        SpringSecurityUtils.doWithAuth("admin") {
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertSame(process, processService.getProcess(process.id))
        }
        // but different user should not have access
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getProcess(process.id)
            }
        }
    }

    /**
     * Tests security of getAllProcessingSteps:
     * user should only have access if ACL defined on JobExecutionPlan
     */
    void testGetAllProcessingStepsSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        // user should not have access to the ProcessingSteps
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getAllProcessingSteps(process)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(processService.getAllProcessingSteps(process).empty)
        }
        // admin should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(processService.getAllProcessingSteps(process).empty)
            // grant read permission to testuser
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(processService.getAllProcessingSteps(process).empty)
        }
        // but a different user should still not have access
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getAllProcessingSteps(process)
            }
        }
    }

    /**
     * Tests security of getNumberOfProcessingSteps:
     * user should only have access if ACL defined on JobExecutionPlan
     */
    void testNumberOfProcessingStepsSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        // user should not have access to the ProcessingSteps
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getNumberOfProcessessingSteps(process)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(0, processService.getNumberOfProcessessingSteps(process))
        }
        // admin should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(0, processService.getNumberOfProcessessingSteps(process))
            // grant read permission to testuser
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(0, processService.getNumberOfProcessessingSteps(process))
        }
        // but a different user should still not have access
        SpringSecurityUtils.doWithAuth("user") {
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
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(processService.getProcessingStep(1))
        }
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(processService.getProcessingStep(1))
        }
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(processService.getProcessingStep(1))
        }
    }

    /**
     * Tests that an operator user has access to all processing steps
     */
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
    void testGetProcessingStepAsAdmin() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        SpringSecurityUtils.doWithAuth("admin") {
            assertSame(step, processService.getProcessingStep(step.id))
        }
    }

    /**
     * Tests that a user has no access to a processing step unless ACL is defined
     * on the JobExecutionPlan
     */
    void testGetProcessingStepAsUser() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        // without ACL it should fail
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getProcessingStep(step.id)
            }
        }
        // now grant the user access
        SpringSecurityUtils.doWithAuth("admin") {
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertSame(step, processService.getProcessingStep(step.id))
        }
        // but different user should not have access
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getProcessingStep(step.id)
            }
        }
    }

    /**
     * Tests the security of getAllUpdates:
     * users should only have access if an ACL is defined on the JobExecutionPlan
     */
    void testGetAllUpdatesSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        // testuser should not have access to the updates
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getAllUpdates(step)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(processService.getAllUpdates(step).empty)
        }
        // but admin user should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(processService.getAllUpdates(step).empty)
            // grant read permission to testuser
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the user should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(processService.getAllUpdates(step).empty)
        }
        // but a different usre should not have access
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getAllUpdates(step)
            }
        }
    }

    /**
     * Tests security of getNumberOfUpdates:
     * user should only have access if ACL defined on JobExecutionPlan
     */
    void testNumberOfUpdatesSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        // user should not have access to the ProcessingStepUpdates
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getNumberOfUpdates(step)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(0, processService.getNumberOfUpdates(step))
        }
        // admin should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(0, processService.getNumberOfUpdates(step))
            // grant read permission to testuser
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(0, processService.getNumberOfUpdates(step))
        }
        // but a different user should still not have access
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getNumberOfUpdates(step)
            }
        }
    }

    /**
     * Tests security of getFinishDateSecurity
     * users should only have access if ACL defined on JobExecutionPlan
     *
     * and that the method behaves correctly in case of not finished processes.
     */
    void testGetFinishDate() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = null
        // user should not have access to the Process's finish date
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getFinishDate(process)
            }
        }
        // admin user should have access
        SpringSecurityUtils.doWithAuth("admin") {
            // process is not finished - it should fail with an IllegalArgumentException
            shouldFail(IllegalArgumentException) {
                processService.getFinishDate(process)
            }
            // let's mark the process as finished (not really correct as it should have a new Update)
            process.finished = true
            assertNotNull(process.save(flush: true))
            // now it should fail with a runtime exception as there are no updates
            shouldFail(RuntimeException) {
                processService.getFinishDate(process)
            }
            // creating the update should help with that exception
            update = mockProcessingStepUpdate(step)
            assertEquals(update.date, processService.getFinishDate(process))
            // grant read permission to testuser
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(update.date, processService.getFinishDate(process))
        }
        // now the testuser should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(update.date, processService.getFinishDate(process))
        }
        // but a different user should still not have access
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getFinishDate(process)
            }
        }
    }

    /**
     * Tests security of getDuration:
     * users should only be allowed to access this method if ACL is defined on JobExecutionPlan
     *
     * In addition the test checks that the method behaves correctly in case of not finished
     * Processes.
     */
    void testGetDuration() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        // user should not have access to the duration
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getDuration(process)
            }
        }
        // but an admin should
        SpringSecurityUtils.doWithAuth("admin") {
            // process is not finished - it should fail with an IllegalArgumentException
            shouldFail(IllegalArgumentException) {
                processService.getDuration(process)
            }
            // let's mark the process as finished (not really correct as it should have a new Update)
            process.finished = true
            assertNotNull(process.save(flush: true))
            // now it should fail with a runtime exception as there are no updates
            shouldFail(RuntimeException) {
                processService.getDuration(process)
            }
        }
        // create the required update
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        long duration = update.date.time - process.started.time
        SpringSecurityUtils.doWithAuth("admin") {
            // now we should have something
            assertEquals(duration, processService.getDuration(process))
            // grant read permission to our testuser
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(duration, processService.getDuration(process))
        }
        // now also the testuser should be able to see it
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(duration, processService.getDuration(process))
        }
        // but another user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getDuration(process)
            }
        }
    }

    /**
     * Tests security of getLatestProcessingStep:
     * user should only have access if ACL defined on JobExecutionPlan
     */
    void testGetLatestProcessingStepSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        // without ACL user should not see the latest ProcessingStep
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getLatestProcessingStep(process)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(processService.getLatestProcessingStep(process))
        }
        // but an admin should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(processService.getLatestProcessingStep(process))
            // let's grant read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(processService.getLatestProcessingStep(process))
        }
        // but another user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getLatestProcessingStep(process)
            }
        }
    }

    /**
     * Tests the security of getState:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    void testGetStateSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        // without ACL user should not be allowed to get the state of the Process
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getState(process)
            }
            shouldFail(AccessDeniedException) {
                processService.getState(step)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(ExecutionState.CREATED, processService.getState(process))
            assertEquals(ExecutionState.CREATED, processService.getState(step))
        }
        // admin user should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(ExecutionState.CREATED, processService.getState(process))
            assertEquals(ExecutionState.CREATED, processService.getState(step))
            // let's grant read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should get the state
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(ExecutionState.CREATED, processService.getState(process))
            assertEquals(ExecutionState.CREATED, processService.getState(step))
        }
        // but another user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getState(process)
            }
            shouldFail(AccessDeniedException) {
                processService.getState(step)
            }
        }
    }

    /**
     * Tests the security of getError:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    void testGetErrorSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        // without ACL user should not be allowed to get the error of the Process
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getError(process)
            }
            shouldFail(AccessDeniedException) {
                processService.getError(step)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(processService.getError(process))
            assertNull(processService.getError(step))
        }
        // admin user should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(processService.getError(process))
            assertNull(processService.getError(step))
            // let's grant read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should get the error
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(processService.getError(process))
            assertNull(processService.getError(step))
        }
        // but another user should not
        SpringSecurityUtils.doWithAuth("user") {
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
    void testGetLastUpdateSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        // without ACL user should not be allowed to get the last update of the Process
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getLastUpdate(process)
            }
            shouldFail(AccessDeniedException) {
                processService.getLastUpdate(step)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(update.date, processService.getLastUpdate(process))
            assertEquals(update.date, processService.getLastUpdate(step))
        }
        // admin user should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(update.date, processService.getLastUpdate(process))
            assertEquals(update.date, processService.getLastUpdate(step))
            // let's grant read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should get the update
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(update.date, processService.getLastUpdate(process))
            assertEquals(update.date, processService.getLastUpdate(step))
        }
        // but another user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getLastUpdate(process)
            }
            shouldFail(AccessDeniedException) {
                processService.getLastUpdate(step)
            }
        }
    }

    /**
     * Tests the security of getLatestProcessingStepUpdate:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    void testGetLatestProcessingStepUpdateSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        // without ACL user should not see the latest ProcessingStepUpdate
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getLatestProcessingStepUpdate(step)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertSame(update, processService.getLatestProcessingStepUpdate(step))
        }
        // admin user should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertSame(update, processService.getLatestProcessingStepUpdate(step))
            // let's grant read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertSame(update, processService.getLatestProcessingStepUpdate(step))
        }
        // but another user should not get the ProcessingStepUpdate
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getLatestProcessingStepUpdate(step)
            }
        }
    }

    /**
     * Tests the security of getFirstUpdate:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    void testGetFirstUpdateSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        // without ACL user should not see the first update time
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getFirstUpdate(step)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(update.date, processService.getFirstUpdate(step))
        }
        // admin user should have access
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(update.date, processService.getFirstUpdate(step))
            // let's grant read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now a user should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(update.date, processService.getFirstUpdate(step))
        }
        // but another user should not get the first update time
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getFirstUpdate(step)
            }
        }
    }

    /**
     * Tests the security of getProcessingStepDuration:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    void testGetProcessingStepDurationSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        // without ACL user should not see the processing step duration
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.getProcessingStepDuration(step)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(processService.getProcessingStepDuration(step))
        }
        // admin user should have access
        SpringSecurityUtils.doWithAuth("admin") {
            // null as we don't have any real updates
            assertNull(processService.getProcessingStepDuration(step))
            // let's grant read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now a user should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(processService.getProcessingStepDuration(step))
        }
        // but another user should not get the processing step duration
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.getProcessingStepDuration(step)
            }
        }
    }

    /**
     * Tests the security of processInformation:
     * only if ACL is defined on JobExecutionPlan a user has access
     */
    void testProcessInformationSecurity() {
        JobExecutionPlan plan = mockPlan()
        StartJobDefinition startJob = new StartJobDefinition(name: "StartJobTest", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save(flush: true))
        plan.startJob = startJob
        assertNotNull(plan.save(flush: true))
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        // without ACL user should not have access to the process information
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.processInformation(process)
            }
        }
        // operator should have access
        SpringSecurityUtils.doWithAuth("operator") {
            processService.processInformation(process)
        }
        // but admin should have access
        SpringSecurityUtils.doWithAuth("admin") {
            // we don't verify the map - needs a dedicated test
            processService.processInformation(process)
            // let's grant read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now our user should have access
        SpringSecurityUtils.doWithAuth("testuser") {
            processService.processInformation(process)
        }
        // but another user should still not get the process information
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                processService.processInformation(process)
            }
        }
    }

    /**
     * Tests the security of restartProcessingStep:
     * requires write permission ACL on JobExecutionPlan
     */
    void testRestartProcessingStepSecurity() {
        JobExecutionPlan plan = mockPlan()
        Process process = mockProcess(plan)
        JobDefinition job = createTestJob("Test", plan)
        ProcessingStep step = mockProcessingStep(process, job)
        ProcessingStepUpdate update = mockProcessingStepUpdate(step)
        // a user without write permission on the plan should not be allowed to restart the ProcessingStep
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                processService.restartProcessingStep(step)
            }
        }
        // operator should be allowed to restart
        SpringSecurityUtils.doWithAuth("operator") {
            // we are in wrong state, so we let if fail on purpose
            shouldFail(IncorrectProcessingException) {
                processService.restartProcessingStep(step)
            }
        }
        // an admin user should be allowed
        SpringSecurityUtils.doWithAuth("admin") {
            // still in wrong state
            shouldFail(IncorrectProcessingException) {
                processService.restartProcessingStep(step)
            }
            // granting read permission should not help
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
            SpringSecurityUtils.doWithAuth("testuser") {
                shouldFail(AccessDeniedException) {
                    processService.restartProcessingStep(step)
                }
            }
            // but granting write permission should
            aclUtilService.addPermission(plan, "testuser", BasePermission.WRITE)
        }
        SpringSecurityUtils.doWithAuth("testuser") {
            // now it should fail with the IncorrectProcessingException
            shouldFail(IncorrectProcessingException) {
                processService.restartProcessingStep(step)
            }
        }
        // but for other users it should still fail
        SpringSecurityUtils.doWithAuth("user") {
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
        stacktraceFile.createNewFile()
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

    private JobExecutionPlan mockPlan(String name = "test") {
        JobExecutionPlan plan = new JobExecutionPlan(name: name, planVersion: 0, enabled: true)
        assertNotNull(plan.save(flush: true))
        return plan
    }

    private Process mockProcess(JobExecutionPlan plan) {
        Process process = new Process(started: new Date(), startJobClass: "foo", startJobVersion: '1', jobExecutionPlan: plan)
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
