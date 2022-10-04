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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.Holders
import org.junit.After
import org.junit.Test
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.jobs.TestSingletonStartJob
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

import static org.junit.Assert.*

@Rollback
@Integration
class JobExecutionPlanServiceIntegrationTests implements UserAndRoles {

    JobExecutionPlanService jobExecutionPlanService

    void setupData() {
        createUserAndRoles()
    }

    @After
    void tearDown() {
        (Holders.applicationContext.getBean("testSingletonStartJob") as TestSingletonStartJob).executionPlan = null
    }

    @Test
    void testWithParentNoParent() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan()
        JobExecutionPlanService service = new JobExecutionPlanService()
        List<JobExecutionPlan> plans = service.withParents(plan)
        assertEquals(1, plans.size())
        assertSame(plan, plans[0])
    }

    @Test
    void testWithParentOneParent() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(obsoleted: true)
        JobExecutionPlan plan2 = new JobExecutionPlan(previousPlan: plan)
        JobExecutionPlanService service = new JobExecutionPlanService()
        // first plan should only return the plan
        List<JobExecutionPlan> plans = service.withParents(plan)
        assertEquals(1, plans.size())
        assertSame(plan, plans[0])
        // second plan should return both with the oldest item first
        plans = service.withParents(plan2)
        assertEquals(2, plans.size())
        assertSame(plan, plans[0])
        assertSame(plan2, plans[1])
    }

    @Test
    void testWithParentManyParent() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(obsoleted: true)
        JobExecutionPlan plan2 = new JobExecutionPlan(previousPlan: plan, obsoleted: true)
        JobExecutionPlan plan3 = new JobExecutionPlan(previousPlan: plan2, obsoleted: true)
        JobExecutionPlan plan4 = new JobExecutionPlan(previousPlan: plan3, obsoleted: true)
        JobExecutionPlan plan5 = new JobExecutionPlan(previousPlan: plan4, obsoleted: true)
        JobExecutionPlan plan6 = new JobExecutionPlan(previousPlan: plan5)
        JobExecutionPlan otherPlan = new JobExecutionPlan()
        JobExecutionPlanService service = new JobExecutionPlanService()
        // first plan should only return the plan
        List<JobExecutionPlan> plans = service.withParents(plan)
        assertEquals(1, plans.size())
        assertSame(plan, plans[0])
        // second plan should return both with the oldest item first
        plans = service.withParents(plan2)
        assertEquals(2, plans.size())
        assertSame(plan, plans[0])
        assertSame(plan2, plans[1])
        // 3rd plan
        plans = service.withParents(plan3)
        assertEquals(3, plans.size())
        assertSame(plan, plans[0])
        assertSame(plan2, plans[1])
        assertSame(plan3, plans[2])
        // 4th plan
        plans = service.withParents(plan4)
        assertEquals(4, plans.size())
        assertSame(plan, plans[0])
        assertSame(plan2, plans[1])
        assertSame(plan3, plans[2])
        assertSame(plan4, plans[3])
        // 5th plan
        plans = service.withParents(plan5)
        assertEquals(5, plans.size())
        assertSame(plan, plans[0])
        assertSame(plan2, plans[1])
        assertSame(plan3, plans[2])
        assertSame(plan4, plans[3])
        assertSame(plan5, plans[4])
        // 6th plan
        plans = service.withParents(plan6)
        assertEquals(6, plans.size())
        assertSame(plan, plans[0])
        assertSame(plan2, plans[1])
        assertSame(plan3, plans[2])
        assertSame(plan4, plans[3])
        assertSame(plan5, plans[4])
        assertSame(plan6, plans[5])
        // other plan should only return itself
        plans = service.withParents(otherPlan)
        assertEquals(1, plans.size())
        assertSame(otherPlan, plans[0])
    }

    @Test
    void testGetAllJobExecutionPlans() {
        setupData()
        JobExecutionPlanService service = new JobExecutionPlanService()
        assertTrue(service.jobExecutionPlans.isEmpty())

        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: true)
        assertNotNull(plan.save(flush: true))
        assertTrue(service.jobExecutionPlans.isEmpty())
        JobExecutionPlan plan2 = new JobExecutionPlan(name: "test", previousPlan: plan, obsoleted: false, planVersion: 1)
        assertNotNull(plan2.save(flush: true))
        List<JobExecutionPlan> plans = service.jobExecutionPlans
        assertEquals(1, plans.size())
        assertSame(plan2, plans[0])

        // adding three other not obsoleted JobExecutionPlans
        JobExecutionPlan plan3 = new JobExecutionPlan(name: "test3", obsoleted: false)
        assertNotNull(plan3.save(flush: true))
        JobExecutionPlan plan4 = new JobExecutionPlan(name: "test4", obsoleted: false)
        assertNotNull(plan4.save(flush: true))
        JobExecutionPlan plan5 = new JobExecutionPlan(name: "test5", obsoleted: false)
        assertNotNull(plan5.save(flush: true))
        // service should return four objects
        plans = service.jobExecutionPlans
        assertEquals(4, plans.size())
        assertTrue(plans.contains(plan2))
        assertTrue(plans.contains(plan3))
        assertTrue(plans.contains(plan4))
        assertTrue(plans.contains(plan5))
        // turn plan3-5 into a sequence
        plan3.obsoleted = true
        assertNotNull(plan3.save(flush: true))
        plan4.obsoleted = true
        plan4.previousPlan = plan3
        plan4.name = "test3"
        plan4.planVersion = 1
        assertNotNull(plan4.save(flush: true))
        plan5.previousPlan = plan4
        plan5.name = "test3"
        plan5.planVersion = 2
        assertNotNull(plan4.save(flush: true))
        // service should return two objects
        plans = service.jobExecutionPlans
        assertEquals(2, plans.size())
        assertTrue(plans.contains(plan2))
        assertTrue(plans.contains(plan5))
    }

    @Test
    void testIsProcessRunningOnePlan() {
        setupData()
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false)
        JobExecutionPlan plan2 = new JobExecutionPlan(name: "test2", obsoleted: false)
        plan.previousPlan = null
        plan2.previousPlan = null
        assertNotNull(plan.save(flush: true))
        assertNotNull(plan2.save(flush: true))
        // has no Process - should return false
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        // create a finished process
        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo")
        Process process2 = new Process(finished: true, jobExecutionPlan: plan2, started: new Date(), startJobClass: "foo")
        assertNotNull(process.save(flush: true))
        assertNotNull(process2.save(flush: true))
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        // adding a third process which is not finished
        Process process3 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo")
        assertNotNull(process3.save(flush: true))
        assertTrue(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
    }

    @Test
    void testIsProcessRunningObsoletedPlan() {
        setupData()
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(id: 1, name: "testIsProcessRunningObsoletedPlan", obsoleted: true, planVersion: 0, previousPlan: null)
        assertNotNull(plan.save(flush: true))
        JobExecutionPlan plan2 = new JobExecutionPlan(id: 2, name: "testIsProcessRunningObsoletedPlan", obsoleted: true, planVersion: 1, previousPlan: plan)
        assertNotNull(plan2.save(flush: true))
        plan.previousPlan = null
        assertNotNull(plan.save(flush: true))
        JobExecutionPlan plan3 = new JobExecutionPlan(id: 3, name: "testIsProcessRunningObsoletedPlan", obsoleted: false, planVersion: 2, previousPlan: plan2)
        assertNotNull(plan3.save(flush: true))
        plan2.previousPlan = plan
        assertNotNull(plan2.save(flush: true))
        plan.previousPlan = null
        assertNotNull(plan.save(flush: true))
        // an unrelated Plan
        JobExecutionPlan plan4 = new JobExecutionPlan(id: 4, name: "otherPlan", obsoleted: false, planVersion: 0)
        assertNotNull(plan4.save(flush: true))
        // no process is running
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        assertFalse(service.isProcessRunning(plan3))
        assertFalse(service.isProcessRunning(plan4))
        // create a finished Process for first Plan
        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo")
        assertNotNull(process.save(flush: true))
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        assertFalse(service.isProcessRunning(plan3))
        assertFalse(service.isProcessRunning(plan4))
        // create a not finished Process for second plan
        Process process2 = new Process(finished: false, jobExecutionPlan: plan2, started: new Date(), startJobClass: "foo")
        assertNotNull(process2.save(flush: true))
        assertFalse(service.isProcessRunning(plan))
        assertTrue(service.isProcessRunning(plan2))
        assertTrue(service.isProcessRunning(plan3))
        assertFalse(service.isProcessRunning(plan4))
        // mark it as finished
        process2.finished = true
        assertNotNull(process2.save(flush: true))
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        assertFalse(service.isProcessRunning(plan3))
        assertFalse(service.isProcessRunning(plan4))
    }

    @Test
    void testGetLastExecutedProcess() {
        setupData()
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        assertNotNull(plan.save(flush: true))
        // there should not be a started Process
        assertNull(service.getLastExecutedProcess(plan))
        // create the process
        Process process1 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo")
        assertNotNull(process1.save(flush: true))
        // there still is no started process
        assertNull(service.getLastExecutedProcess(plan))
        // create the Processing Step
        JobDefinition jobDefinition1 = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition1.save(flush: true))
        plan.firstJob = jobDefinition1
        assertNotNull(plan.save(flush: true))
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition1)
        assertNotNull(step1.save(flush: true))
        // there still is no started Process
        assertNull(service.getLastExecutedProcess(plan))
        // now a created Processing Step Update...
        ProcessingStepUpdate created1 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step1)
        assertNotNull(created1.save(flush: true))
        // ... which changes the world
        assertSame(process1, service.getLastExecutedProcess(plan))
        // adding a success should not change anything
        ProcessingStepUpdate success1 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(created1), processingStep: step1, previous: created1)
        assertNotNull(success1.save(flush: true))
        assertSame(process1, service.getLastExecutedProcess(plan))
        // but adding a second process will change
        Process process2 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo")
        assertNotNull(process2.save(flush: true))
        ProcessingStep step2 = new ProcessingStep(process: process2, jobDefinition: jobDefinition1)
        assertNotNull(step2.save(flush: true))
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(success1), processingStep: step2)
        assertNotNull(created2.save(flush: true))
        // now it should be Process2
        assertSame(process2, service.getLastExecutedProcess(plan))
        // adding a success should not change anything
        ProcessingStepUpdate success2 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(created2), processingStep: step2, previous: created2)
        assertNotNull(success2.save(flush: true))
        assertSame(process2, service.getLastExecutedProcess(plan))
        // even adding a new Processing Step to Process 1 should not change anything
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "foo", plan: plan, previous: jobDefinition1)
        assertNotNull(jobDefinition2.save(flush: true))
        jobDefinition1.next = jobDefinition2
        assertNotNull(jobDefinition1.save(flush: true))
        ProcessingStep step3 = new ProcessingStep(process: process1, jobDefinition: jobDefinition2, previous: step1)
        assertNotNull(step3.save(flush: true))
        ProcessingStepUpdate created3 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(success2), processingStep: step3)
        assertNotNull(created3.save(flush: true))
        assertSame(process2, service.getLastExecutedProcess(plan))
    }

    @Test
    void testGetLatestUpdatesForPlanInStateFailure() {
        setupData()
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        assertNotNull(plan.save(flush: true))
        def planData = service.getLatestUpdatesForPlan(plan)
        assertTrue(planData.isEmpty())
        // create the process
        Process process1 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo")
        assertNotNull(process1.save(flush: true))
        // create the Processing Step
        JobDefinition jobDefinition1 = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition1.save(flush: true))
        plan.firstJob = jobDefinition1
        assertNotNull(plan.save(flush: true))
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition1)
        assertNotNull(step1.save(flush: true))
        // now a created Processing Step Update...
        mockProcessingStepAsFinished(step1)
        // latest updates for plan should show one
        planData = service.getLatestUpdatesForPlan(plan)
        assertFalse(planData.isEmpty())
        assertTrue(planData.containsKey(process1))
        assertEquals(planData.get(process1).state, ExecutionState.FINISHED)
        // lets restrict the returned data
        assertEquals(1, service.getNumberOfProcesses(plan, [ExecutionState.FINISHED]))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, [ExecutionState.FINISHED])
        assertFalse(planData.isEmpty())
        assertTrue(planData.containsKey(process1))
        assertEquals(planData.get(process1).state, ExecutionState.FINISHED)
        // try to restrict on something else
        assertEquals(0, service.getNumberOfProcesses(plan,  [ExecutionState.CREATED]))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, [ExecutionState.CREATED])
        assertTrue(planData.isEmpty())
        // lets create a failure for the processing step
        ProcessingStepUpdate update = CollectionUtils.atMostOneElement(ProcessingStepUpdate.findAllByState(ExecutionState.FINISHED))
        ProcessingStepUpdate failure = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FAILURE,
            previous: update,
            processingStep: update.processingStep
        )
        assertNotNull(failure.save(flush: true))
        // test for failure
        assertEquals(1, service.getNumberOfProcesses(plan,  [ExecutionState.FAILURE]))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, [ExecutionState.FAILURE])
        assertFalse(planData.isEmpty())
        assertTrue(planData.containsKey(process1))
        assertEquals(planData.get(process1), failure)
        // create a restarted update
        ProcessingStepUpdate restarted = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.RESTARTED,
            previous: failure,
            processingStep: update.processingStep
        )
        assertNotNull(restarted.save(flush: true))
        // test for failure
        assertEquals(0, service.getNumberOfProcesses(plan,  [ExecutionState.FAILURE]))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, [ExecutionState.FAILURE])
        assertTrue(planData.isEmpty())
        // test for restarted
        assertEquals(1, service.getNumberOfProcesses(plan,  [ExecutionState.RESTARTED]))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, [ExecutionState.RESTARTED])
        assertFalse(planData.isEmpty())
        assertTrue(planData.containsKey(process1))
        assertEquals(planData.get(process1), restarted)
        // test generic
        assertEquals(1, service.getNumberOfProcesses(plan))
        planData = service.getLatestUpdatesForPlan(plan)
        assertFalse(planData.isEmpty())
        assertTrue(planData.containsKey(process1))
        assertEquals(planData.get(process1), restarted)
    }

    /**
     * Test that verifies that enabling a JobExecutionPlan properly updates the state, that is
     * it is in enabled state afterwards.
     */
    @Test
    void testEnablePlan() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        assertFalse(plan.enabled)
        // let's enable the Plan
        doWithAuth(ADMIN) {
            jobExecutionPlanService.enablePlan(plan)
        }
        assertTrue(plan.enabled)
        // enabling the plan again should not change anything
        doWithAuth(ADMIN) {
            jobExecutionPlanService.enablePlan(plan)
        }
        assertTrue(plan.enabled)
    }

    @Test
    void testEnablePlanForStartJob() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        plan = plan.save(flush: true)
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save(flush: true))
        plan.startJob = startJob
        assertNotNull(plan.save(flush: true))

        // get a startJob Instance for the testStartJob and inject the JobExecutionPlan
        TestSingletonStartJob job = null
        job = Holders.applicationContext.getBean("testSingletonStartJob") as TestSingletonStartJob
        job.executionPlan = plan
        assertNotNull(job)
        // everything should be disabled now
        assertFalse(plan.enabled)
        assertFalse(job.jobExecutionPlan.enabled)
        // let's enable the Plan
        doWithAuth(ADMIN) {
            jobExecutionPlanService.enablePlan(plan)
        }
        assertTrue(plan.enabled)
        assertTrue(job.jobExecutionPlan.enabled)
    }

    @Test
    void testDisablePlan() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        assertTrue(plan.enabled)
        // let's disable the Plan
        doWithAuth(ADMIN) {
            jobExecutionPlanService.disablePlan(plan)
        }
        assertFalse(plan.enabled)
        // disabling the plan again should not change anything
        doWithAuth(ADMIN) {
            jobExecutionPlanService.disablePlan(plan)
        }
        assertFalse(plan.enabled)
    }

    @Test
    void testDisablePlanForStartJob() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save(flush: true))
        plan.startJob = startJob
        assertNotNull(plan.save(flush: true))

        // get a startJob Instance for the testStartJob and inject the JobExecutionPlan
        TestSingletonStartJob job = null
        job = Holders.applicationContext.getBean("testSingletonStartJob") as TestSingletonStartJob
        job.executionPlan = plan
        assertNotNull(job)
        // everything should be enabled now
        assertTrue(plan.enabled)
        assertTrue(job.jobExecutionPlan.enabled)
        // let's disable the Plan
        doWithAuth(ADMIN) {
            jobExecutionPlanService.disablePlan(plan)
        }
        assertFalse(plan.enabled)
        assertFalse(job.jobExecutionPlan.enabled)
    }

    @Test
    void testGetPlanSecurity() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            assertSame(plan, jobExecutionPlanService.getPlan(plan.id))
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getPlan(plan.id)
            }
        }
    }

    @Test
    void testEnablePlanSecurity() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            jobExecutionPlanService.enablePlan(plan)
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.enablePlan(plan)
            }
        }
    }

    @Test
    void testDisablePlanSecurity() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            jobExecutionPlanService.disablePlan(plan)
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.disablePlan(plan)
            }
        }
    }

    @Test
    void testGetAllPlansPermission() {
        setupData()
        int numberOfPlans = 3
        numberOfPlans.times {
            JobExecutionPlan plan = new JobExecutionPlan(name: "test${it}", obsoleted: false, planVersion: 0, enabled: true)
            plan.save(flush: true)
            assertNotNull(plan)
        }

        doWithAuth(OPERATOR) {
            assertEquals(numberOfPlans, jobExecutionPlanService.jobExecutionPlans.size())
        }
        doWithAuth(TESTUSER) {
            assertTrue(jobExecutionPlanService.jobExecutionPlans.empty)
        }
    }

    @Test
    void testGetAllProcessesPermission() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            assertTrue(jobExecutionPlanService.getAllProcesses(plan).empty)
        }
        doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getAllProcesses(plan)
            }
        }
    }

    @Test
    void testGetLatestUpdatesForPlanSecurity() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            assertNull(jobExecutionPlanService.getLatestUpdatesForPlan(plan).empty)
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLatestUpdatesForPlan(plan)
            }
        }
    }

    @Test
    void testIsProcessRunningSecurity() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            assertFalse(jobExecutionPlanService.isProcessRunning(plan))
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.isProcessRunning(plan)
            }
        }
    }

    @Test
    void testGetProcessCountSecurity() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            assertEquals(0, jobExecutionPlanService.getProcessCount(plan))
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getProcessCount(plan)
            }
        }
    }

    @Test
    void testGetLastExecutedProcessSecurity() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            assertNull(jobExecutionPlanService.getLastExecutedProcess(plan))
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastExecutedProcess(plan)
            }
        }
    }

    @Test
    void testGetNumberOfProcessesSecurity() {
        setupData()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)

        doWithAuth(OPERATOR) {
            assertEquals(0, jobExecutionPlanService.getNumberOfProcesses(plan))
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getNumberOfProcesses(plan)
            }
        }
    }

    @Test
    void testPlanInformation() {
        setupData()
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save(flush: true))
        plan.startJob = startJob
        plan.firstJob = startJob
        assertNotNull(plan.save(flush: true))

        doWithAuth(OPERATOR) {
            jobExecutionPlanService.planInformation(plan)
        }
        doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                jobExecutionPlanService.planInformation(plan)
            }
        }
    }

    private Date nextDate(ProcessingStepUpdate update) {
        return new Date(update.date.time + 1)
    }

    private static ProcessingStepUpdate mockProcessingStepAsFinished(ProcessingStep step) {
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.STARTED,
            previous: update,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FINISHED,
            previous: update,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        return update
    }

    private static ProcessingStepUpdate mockProcessingStepAsState(ProcessingStep step, ExecutionState state) {
        ProcessingStepUpdate update = mockProcessingStepAsFinished(step)
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: state,
            previous: update,
            processingStep: step
        )
        assertNotNull(update.save(flush: true))
        return update
    }

    @Test
    void "test processCount"() {
        setup:
        setupData()
        createJepsWithProcesses()

        when:
        Map<String, Long> result = doWithAuth(ADMIN) {
            jobExecutionPlanService.processCount()
        }

        then:
        assert result == [fastqcQualityAssessment: 2L, conveySamplePairConsistency: 2L]
    }

    @Test
    void "test finishedProcessCount"() {
        setup:
        setupData()
        createJepsWithProcesses()

        when:
        Map<String, Long> result = doWithAuth(ADMIN) {
            jobExecutionPlanService.finishedProcessCount()
        }

        then:
        assert result == [fastqcQualityAssessment: 1L, conveySamplePairConsistency: 2L]
    }

    private static void createJepsWithProcesses() {
        // JEP with two processes, one finished
        def jep1 = DomainFactory.createJobExecutionPlan(name: "fastqcQualityAssessment")
        DomainFactory.createProcess(jobExecutionPlan: jep1, finished: true)
        DomainFactory.createProcess(jobExecutionPlan: jep1, finished: false)

        // JEP with one process and an obsolete predecessor JEP with one process, both finished
        def obsoleteJep2 = DomainFactory.createJobExecutionPlan(name: "conveySamplePairConsistency", obsoleted: true, planVersion: 0)
        DomainFactory.createProcess(jobExecutionPlan: obsoleteJep2, finished: true)
        def jep2 = DomainFactory.createJobExecutionPlan(name: "conveySamplePairConsistency", planVersion: 1, previousPlan: obsoleteJep2)
        DomainFactory.createProcess(jobExecutionPlan: jep2, finished: true)

        // JEP without processes
        DomainFactory.createJobExecutionPlan(name: "SnvAlignmentDiscovery")
    }

    @Test
    void "test failProcessCount"() {
        setup:
        setupData()
        // JEP with an obsolete predecessor JEP
        JobExecutionPlan plan1a = DomainFactory.createJobExecutionPlan(name: "a", obsoleted: true, planVersion: 0)
        JobExecutionPlan plan1b = DomainFactory.createJobExecutionPlan(name: "a", planVersion: 1, previousPlan: plan1a)
        createProcessingStepHelper(plan1a, ExecutionState.FAILURE)
        createProcessingStepHelper(plan1b, ExecutionState.FAILURE)

        // JEP without processes
        DomainFactory.createJobExecutionPlan(name: "b")

        // process has fail and afterwards finished state
        JobExecutionPlan plan3 = DomainFactory.createJobExecutionPlan(name: "c")
        DomainFactory.createProcessingStepUpdate(createProcessingStepHelper(plan3, ExecutionState.FAILURE), ExecutionState.FINISHED)

        // process with restarted job
        JobExecutionPlan plan4 = DomainFactory.createJobExecutionPlan(name: "d")
        DomainFactory.createProcessingStepUpdate(
                DomainFactory.createProcessingStepUpdate(
                        DomainFactory.createRestartedProcessingStep([
                                original: createProcessingStepHelper(plan4, ExecutionState.FAILURE)
                        ]),
                        ExecutionState.CREATED).processingStep,
                ExecutionState.FINISHED)

        // process in different states
        JobExecutionPlan plan5 = DomainFactory.createJobExecutionPlan(name: "e")
        createProcessingStepHelper(plan5, ExecutionState.SUCCESS)
        createProcessingStepHelper(plan5, ExecutionState.FINISHED)
        createProcessingStepHelper(plan5, ExecutionState.RESTARTED)
        createProcessingStepHelper(plan5, ExecutionState.RESUMED)
        createProcessingStepHelper(plan5, ExecutionState.STARTED)
        createProcessingStepHelper(plan5, ExecutionState.SUSPENDED)

        when:
        Map<String, Long> result = doWithAuth(ADMIN) {
            jobExecutionPlanService.failedProcessCount(jobExecutionPlanService.jobExecutionPlansWithPreviousVersions)
        }

        then:
        assert result == [a: 2L]
    }

    ProcessingStep createProcessingStepHelper(JobExecutionPlan plan, ExecutionState state) {
        ProcessingStep step = DomainFactory.createProcessingStep(
                process: DomainFactory.createProcess(
                        jobExecutionPlan: plan
                )
        )
        DomainFactory.createProcessingStepUpdate(step, ExecutionState.CREATED)
        DomainFactory.createProcessingStepUpdate(step, state)
        return step
    }

    @Test
    void "test lastProcessDate, finds date of newest update with the given state"() {
        setup:
        setupData()
        // create a JEP with processes, processing states and updates
        Date date = createLastDateStructure("panCanInstallation", ExecutionState.SUCCESS)

        when:
        Map<String, Date> result = doWithAuth(ADMIN) {
            jobExecutionPlanService.lastProcessDate(jobExecutionPlanService.jobExecutionPlansWithPreviousVersions, ExecutionState.SUCCESS)
        }

        then:
        assert result == [panCanInstallation: date]
    }

    private static Date createLastDateStructure(String planName, ExecutionState state) {
        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan(name: planName)
        Process proc = DomainFactory.createProcess(jobExecutionPlan: jep, finished: true)
        ProcessingStep step = DomainFactory.createProcessingStep(process: proc, next: null)
        Date date = mockProcessingStepAsState(step, state).date

        Process proc2 = DomainFactory.createProcess(jobExecutionPlan: jep, finished: false)
        ProcessingStep step2 = DomainFactory.createProcessingStep(process: proc2, next: null)
        mockProcessingStepAsState(step2, state)

        Process proc3 = DomainFactory.createProcess(jobExecutionPlan: jep, finished: true)
        ProcessingStep nextStep = DomainFactory.createProcessingStep(process: proc3, next: null)
        ProcessingStep step3 = DomainFactory.createProcessingStep(process: proc3, next: nextStep)
        mockProcessingStepAsState(step3, state)
        return date
    }
}
