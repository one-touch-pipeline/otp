package de.dkfz.tbi.otp.job.processing

import static org.junit.Assert.*

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import de.dkfz.tbi.otp.testing.TestSingletonStartJob

import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.BasePermission

class JobExecutionPlanServiceTests extends AbstractIntegrationTest  {
    def jobExecutionPlanService
    def grailsApplication

    void setUp() {
        createUserAndRoles()
    }

    void tearDown() {
        (grailsApplication.mainContext.getBean("testSingletonStartJob") as TestSingletonStartJob).setExecutionPlan(null)
    }

    @Test
    void testWithParentNoParent() {
        JobExecutionPlan plan = new JobExecutionPlan()
        JobExecutionPlanService service = new JobExecutionPlanService()
        List<JobExecutionPlan> plans = service.withParents(plan)
        assertEquals(1, plans.size())
        assertSame(plan, plans[0])
    }

    @Test
    void testWithParentOneParent() {
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
        JobExecutionPlanService service = new JobExecutionPlanService()
        assertTrue(service.getAllJobExecutionPlans().isEmpty())

        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: true)
        assertNotNull(plan.save())
        assertTrue(service.getAllJobExecutionPlans().isEmpty())
        JobExecutionPlan plan2 = new JobExecutionPlan(name: "test2", previousPlan: plan, obsoleted: false, planVersion: 1)
        assertNotNull(plan2.save())
        List<JobExecutionPlan> plans = service.getAllJobExecutionPlans()
        assertEquals(1, plans.size())
        assertSame(plan2, plans[0])

        // adding three other not obsoleted JobExecutionPlans
        JobExecutionPlan plan3 = new JobExecutionPlan(name: "test3", obsoleted: false)
        assertNotNull(plan3.save())
        JobExecutionPlan plan4 = new JobExecutionPlan(name: "test4", obsoleted: false)
        assertNotNull(plan4.save())
        JobExecutionPlan plan5 = new JobExecutionPlan(name: "test5", obsoleted: false)
        assertNotNull(plan5.save())
        // service should return four objects
        plans = service.getAllJobExecutionPlans()
        assertEquals(4, plans.size())
        assertTrue(plans.contains(plan2))
        assertTrue(plans.contains(plan3))
        assertTrue(plans.contains(plan4))
        assertTrue(plans.contains(plan5))
        // turn plan3-5 into a sequence
        plan3.obsoleted = true
        assertNotNull(plan3.save())
        plan4.obsoleted = true
        plan4.previousPlan = plan3
        plan4.planVersion = 1
        assertNotNull(plan4.save())
        plan5.previousPlan = plan4
        plan5.planVersion = 2
        assertNotNull(plan4.save())
        // service should return two objects
        plans = service.getAllJobExecutionPlans()
        assertEquals(2, plans.size())
        assertTrue(plans.contains(plan2))
        assertTrue(plans.contains(plan5))
    }

    @Test
    void testIsProcessRunningOnePlan() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false)
        JobExecutionPlan plan2 = new JobExecutionPlan(name: "test2", obsoleted: false)
        plan.previousPlan = null
        plan2.previousPlan = null
        assertNotNull(plan.save())
        assertNotNull(plan2.save())
        // has no Process - should return false
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        // create a finished process
        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process2 = new Process(finished: true, jobExecutionPlan: plan2, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process.save())
        assertNotNull(process2.save())
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        // adding a third process which is not finished
        Process process3 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process3.save())
        assertTrue(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
    }

    @Test
    void testIsProcessRunningObsoletedPlan() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(id: 1, name: "testIsProcessRunningObsoletedPlan", obsoleted: true, planVersion: 0, previousPlan: null)
        assertNotNull(plan.save())
        JobExecutionPlan plan2 = new JobExecutionPlan(id: 2, name: "testIsProcessRunningObsoletedPlan", obsoleted: true, planVersion: 1, previousPlan: plan)
        assertNotNull(plan2.save())
        plan.previousPlan = null
        assertNotNull(plan.save())
        JobExecutionPlan plan3 = new JobExecutionPlan(id: 3, name: "testIsProcessRunningObsoletedPlan", obsoleted: false, planVersion: 2, previousPlan: plan2)
        assertNotNull(plan3.save())
        plan2.previousPlan = plan
        assertNotNull(plan2.save())
        plan.previousPlan = null
        assertNotNull(plan.save())
        // an unrelated Plan
        JobExecutionPlan plan4 = new JobExecutionPlan(id: 4, name: "otherPlan", obsoleted: false, planVersion: 0)
        assertNotNull(plan4.save())
        // no process is running
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        assertFalse(service.isProcessRunning(plan3))
        assertFalse(service.isProcessRunning(plan4))
        // create a finished Process for first Plan
        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process.save())
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        assertFalse(service.isProcessRunning(plan3))
        assertFalse(service.isProcessRunning(plan4))
        // create a not finished Process for second plan
        Process process2 = new Process(finished: false, jobExecutionPlan: plan2, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process2.save())
        assertFalse(service.isProcessRunning(plan))
        assertTrue(service.isProcessRunning(plan2))
        assertTrue(service.isProcessRunning(plan3))
        assertFalse(service.isProcessRunning(plan4))
        // mark it as finished
        process2.finished = true
        assertNotNull(process2.save())
        assertFalse(service.isProcessRunning(plan))
        assertFalse(service.isProcessRunning(plan2))
        assertFalse(service.isProcessRunning(plan3))
        assertFalse(service.isProcessRunning(plan4))
    }

    @Test
    void testGetLastSucceededProcess() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastSucceededProcess", obsoleted: true, planVersion: 0)
        assertNotNull(plan.save())
        // no Process created - should be null
        assertNull(service.getLastSucceededProcess(plan))
        // create a Process - but not with any Processing Steps
        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process.save())
        assertNull(service.getLastSucceededProcess(plan))
        // create a ProcessingStep for this Process - but not succeeded
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition.save())
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        assertNotNull(step.save())
        assertNull(service.getLastSucceededProcess(plan))
        // add a ProcessingStepUpdate to this Process - should not change anything
        ProcessingStepUpdate created = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step)
        assertNotNull(created.save())
        assertNull(service.getLastSucceededProcess(plan))
        // now lets try to add a success event
        ProcessingStepUpdate success = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: new Date(), processingStep: step, previous: created)
        assertNotNull(success.save())
        assertSame(process, service.getLastSucceededProcess(plan))
        // now we are evil and add another processing step
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "foo", plan: plan, previous: jobDefinition)
        assertNotNull(jobDefinition2)
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition2.save())
        ProcessingStep step2 = new ProcessingStep(process: process, jobDefinition: jobDefinition2, next: null)
        assertNotNull(step2.save())
        step.next = step2
        assertNotNull(step.save())
        // lets fail
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step2)
        assertNotNull(created2.save())
        assertNull(service.getLastSucceededProcess(plan))
    }

    @Test
    void testGetLastSucceededProcessMultipleProcesses() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastSucceededProcessMultipleProcesses", obsoleted: true, planVersion: 0)
        assertNotNull(plan.save())
        // let's create some processes
        Process process1 = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process2 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process3 = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process4 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process5 = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process1.save())
        assertNotNull(process2.save())
        assertNotNull(process3.save())
        assertNotNull(process4.save())
        assertNotNull(process5.save())
        // let's create a processing step for each of the Processes
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition.save())
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition)
        assertNotNull(step1.save())
        ProcessingStep step2 = new ProcessingStep(process: process2, jobDefinition: jobDefinition)
        assertNotNull(step2.save())
        ProcessingStep step3 = new ProcessingStep(process: process3, jobDefinition: jobDefinition)
        assertNotNull(step3.save())
        ProcessingStep step4 = new ProcessingStep(process: process4, jobDefinition: jobDefinition)
        assertNotNull(step4.save())
        ProcessingStep step5 = new ProcessingStep(process: process5, jobDefinition: jobDefinition)
        assertNotNull(step5.save())
        // so far nothing should be done - so no process succeeded
        assertNull(service.getLastSucceededProcess(plan))
        // now a created processing step update for each
        ProcessingStepUpdate created1 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step1)
        assertNotNull(created1.save())
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created1), processingStep: step2)
        assertNotNull(created2.save())
        ProcessingStepUpdate created3 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created2), processingStep: step3)
        assertNotNull(created3.save())
        ProcessingStepUpdate created4 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created3), processingStep: step4)
        assertNotNull(created4.save())
        ProcessingStepUpdate created5 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created4), processingStep: step5)
        assertNotNull(created5.save())
        // so far nothing should be done - so no process succeeded
        assertNull(service.getLastSucceededProcess(plan))
        // let's create a success for process1
        ProcessingStepUpdate success1 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(created5), processingStep: step1, previous: created1)
        assertNotNull(success1.save())
        assertSame(process1, service.getLastSucceededProcess(plan))
        // let's create a success for process5
        ProcessingStepUpdate success5 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(success1), processingStep: step5, previous: created5)
        assertNotNull(success5.save())
        assertSame(process5, service.getLastSucceededProcess(plan))
        // let's create a success for process4 - it is not finished, so it shouldn't change anything
        ProcessingStepUpdate success4 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(success5), processingStep: step4, previous: created4)
        assertNotNull(success4.save())
        assertSame(process5, service.getLastSucceededProcess(plan))
        // last but not least create a success for process3 - we create it before success5, so it should not change anything
        ProcessingStepUpdate success3 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(created5), processingStep: step3, previous: created3)
        assertNotNull(success3.save())
        assertSame(process5, service.getLastSucceededProcess(plan))
    }

    @Test
    void testGetLastSucceededProcessMultiplePlans() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan1 = new JobExecutionPlan(id: 1, name: "testGetLastSucceededProcessMultiplePlans", obsoleted: true, planVersion: 0)
        assertNotNull(plan1.save())
        JobExecutionPlan plan2 = new JobExecutionPlan(id: 2, name: "testGetLastSucceededProcessMultiplePlans", obsoleted: true, planVersion: 1, previousPlan: plan1)
        assertNotNull(plan2.save())
        plan1.previousPlan = null
        assertNotNull(plan1.save())
        JobExecutionPlan plan3 = new JobExecutionPlan(id: 3, name: "testGetLastSucceededProcessMultiplePlans", obsoleted: false, planVersion: 2, previousPlan: plan2)
        assertNotNull(plan3.save())
        plan2.previousPlan = plan1
        assertNotNull(plan2.save())
        plan1.previousPlan = null
        assertNotNull(plan1.save())
        // an unrelated Plan
        JobExecutionPlan plan4 = new JobExecutionPlan(id: 4, name: "otherPlan", obsoleted: false, planVersion: 0)
        assertNotNull(plan4.save())
        // let's create some processes
        Process process1 = new Process(finished: true, jobExecutionPlan: plan1, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process2 = new Process(finished: true, jobExecutionPlan: plan2, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process3 = new Process(finished: true, jobExecutionPlan: plan3, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process4 = new Process(finished: true, jobExecutionPlan: plan4, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process1.save())
        assertNotNull(process2.save())
        assertNotNull(process3.save())
        assertNotNull(process4.save())
        // for none of the plans there should be a succeeded
        assertNull(service.getLastSucceededProcess(plan1))
        assertNull(service.getLastSucceededProcess(plan2))
        assertNull(service.getLastSucceededProcess(plan3))
        assertNull(service.getLastSucceededProcess(plan4))
        // now create some Processing Steps
        JobDefinition jobDefinition1 = new JobDefinition(name: "test", bean: "foo", plan: plan1)
        assertNotNull(jobDefinition1.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "test", bean: "foo", plan: plan2)
        assertNotNull(jobDefinition2.save())
        JobDefinition jobDefinition3 = new JobDefinition(name: "test", bean: "foo", plan: plan3)
        assertNotNull(jobDefinition3.save())
        JobDefinition jobDefinition4 = new JobDefinition(name: "test", bean: "foo", plan: plan4)
        assertNotNull(jobDefinition4.save())
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition1)
        assertNotNull(step1.save())
        ProcessingStep step2 = new ProcessingStep(process: process2, jobDefinition: jobDefinition2)
        assertNotNull(step2.save())
        ProcessingStep step3 = new ProcessingStep(process: process3, jobDefinition: jobDefinition3)
        assertNotNull(step3.save())
        ProcessingStep step4 = new ProcessingStep(process: process4, jobDefinition: jobDefinition4)
        assertNotNull(step4.save())
        // now a created processing step update for each
        ProcessingStepUpdate created1 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step1)
        assertNotNull(created1.save())
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created1), processingStep: step2)
        assertNotNull(created2.save())
        ProcessingStepUpdate created3 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created2), processingStep: step3)
        assertNotNull(created3.save())
        ProcessingStepUpdate created4 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created3), processingStep: step4)
        assertNotNull(created4.save())
        // there should still be nothing
        assertNull(service.getLastSucceededProcess(plan1))
        assertNull(service.getLastSucceededProcess(plan2))
        assertNull(service.getLastSucceededProcess(plan3))
        assertNull(service.getLastSucceededProcess(plan4))
        // create a success for ProcessingStep 4
        ProcessingStepUpdate success4 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(created4), processingStep: step4, previous: created4)
        assertNotNull(success4.save())
        // should render process4 for plan4
        assertNull(service.getLastSucceededProcess(plan1))
        assertNull(service.getLastSucceededProcess(plan2))
        assertNull(service.getLastSucceededProcess(plan3))
        assertSame(process4, service.getLastSucceededProcess(plan4))
        // now create a success for ProcessingStep 3
        ProcessingStepUpdate success3 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(success4), processingStep: step3, previous: created3)
        assertNotNull(success3.save())
        assertNull(service.getLastSucceededProcess(plan1))
        assertNull(service.getLastSucceededProcess(plan2))
        assertSame(process3, service.getLastSucceededProcess(plan3))
        assertSame(process4, service.getLastSucceededProcess(plan4))
        // now one for ProcessingStep1
        ProcessingStepUpdate success1 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(success3), processingStep: step1, previous: created1)
        assertNotNull(success1.save())
        assertSame(process1, service.getLastSucceededProcess(plan1))
        assertSame(process1, service.getLastSucceededProcess(plan2))
        assertSame(process1, service.getLastSucceededProcess(plan3))
        assertSame(process4, service.getLastSucceededProcess(plan4))
        // last but not least for ProcessingStep2
        ProcessingStepUpdate success2 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(success1), processingStep: step2, previous: created2)
        assertNotNull(success2.save())
        assertSame(process1, service.getLastSucceededProcess(plan1))
        assertSame(process2, service.getLastSucceededProcess(plan2))
        assertSame(process2, service.getLastSucceededProcess(plan3))
        assertSame(process4, service.getLastSucceededProcess(plan4))
    }

    @Test
    void testGetLastFailedProcess() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFailedProcess", obsoleted: true, planVersion: 0)
        assertNotNull(plan.save())
        // no Process created - should be null
        assertNull(service.getLastFailedProcess(plan))
        // create a Process - but not with any Processing Steps
        Process process = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process.save())
        assertNull(service.getLastFailedProcess(plan))
        // create a ProcessingStep for this Process - but not succeeded
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition.save())
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        assertNotNull(step.save())
        assertNull(service.getLastFailedProcess(plan))
        // add a ProcessingStepUpdate to this Process - should not change anything
        ProcessingStepUpdate created = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step)
        assertNotNull(created.save())
        assertNull(service.getLastFailedProcess(plan))
        // now lets try to add a failure event
        ProcessingStepUpdate success = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: new Date(), processingStep: step, previous: created)
        assertNotNull(success.save())
        assertSame(process, service.getLastFailedProcess(plan))
        // now we are evil and add another processing step
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "foo", plan: plan, previous: jobDefinition)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        ProcessingStep step2 = new ProcessingStep(process: process, jobDefinition: jobDefinition2, next: null)
        assertNotNull(step2.save())
        step.next = step2
        assertNotNull(step.save())
        // lets fail
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step2)
        assertNotNull(created2.save())
        assertNull(service.getLastFailedProcess(plan))
    }

    @Test
    void testGetLastFailedProcessMultipleProcesses() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFailedProcessMultipleProcesses", obsoleted: true, planVersion: 0)
        assertNotNull(plan.save())
        // let's create some processes
        Process process1 = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process2 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process3 = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process4 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process5 = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process1.save())
        assertNotNull(process2.save())
        assertNotNull(process3.save())
        assertNotNull(process4.save())
        assertNotNull(process5.save())
        // let's create a processing step for each of the Processes
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition.save())
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition)
        assertNotNull(step1.save())
        ProcessingStep step2 = new ProcessingStep(process: process2, jobDefinition: jobDefinition)
        assertNotNull(step2.save())
        ProcessingStep step3 = new ProcessingStep(process: process3, jobDefinition: jobDefinition)
        assertNotNull(step3.save())
        ProcessingStep step4 = new ProcessingStep(process: process4, jobDefinition: jobDefinition)
        assertNotNull(step4.save())
        ProcessingStep step5 = new ProcessingStep(process: process5, jobDefinition: jobDefinition)
        assertNotNull(step5.save())
        // so far nothing should be done - so no process failed
        assertNull(service.getLastFailedProcess(plan))
        // now a created processing step update for each
        ProcessingStepUpdate created1 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step1)
        assertNotNull(created1.save())
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created1), processingStep: step2)
        assertNotNull(created2.save())
        ProcessingStepUpdate created3 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created2), processingStep: step3)
        assertNotNull(created3.save())
        ProcessingStepUpdate created4 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created3), processingStep: step4)
        assertNotNull(created4.save())
        ProcessingStepUpdate created5 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created4), processingStep: step5)
        assertNotNull(created5.save())
        // so far nothing should be done - so no process failed
        assertNull(service.getLastSucceededProcess(plan))
        // let's create a failure for process1
        ProcessingStepUpdate failure1 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(created5), processingStep: step1, previous: created1)
        assertNotNull(failure1.save())
        assertSame(process1, service.getLastFailedProcess(plan))
        // let's create a failure for process5
        ProcessingStepUpdate failure5 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(failure1), processingStep: step5, previous: created5)
        assertNotNull(failure5.save())
        assertSame(process5, service.getLastFailedProcess(plan))
        // let's create a failure for process4 - it is not finished, so it shouldn't change anything
        ProcessingStepUpdate failure4 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(failure5), processingStep: step4, previous: created4)
        assertNotNull(failure4.save())
        assertSame(process5, service.getLastFailedProcess(plan))
        // last but not least create a failure for process3 - we create it before failure5, so it should not change anything
        ProcessingStepUpdate failure3 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(created5), processingStep: step3, previous: created3)
        assertNotNull(failure3.save())
        assertSame(process5, service.getLastFailedProcess(plan))
    }

    @Test
    void testGetLastFailedProcessMultiplePlans() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan1 = new JobExecutionPlan(id: 1, name: "testGetLastFailedProcessMultiplePlans", obsoleted: true, planVersion: 0)
        assertNotNull(plan1.save())
        JobExecutionPlan plan2 = new JobExecutionPlan(id: 2, name: "testGetLastFailedProcessMultiplePlans", obsoleted: true, planVersion: 1, previousPlan: plan1)
        assertNotNull(plan2.save())
        plan1.previousPlan = null
        assertNotNull(plan1.save())
        JobExecutionPlan plan3 = new JobExecutionPlan(id: 3, name: "testGetLastFailedProcessMultiplePlans", obsoleted: false, planVersion: 2, previousPlan: plan2)
        assertNotNull(plan3.save())
        plan2.previousPlan = plan1
        assertNotNull(plan2.save())
        plan1.previousPlan = null
        assertNotNull(plan1.save())
        // an unrelated Plan
        JobExecutionPlan plan4 = new JobExecutionPlan(id: 4, name: "otherPlan", obsoleted: false, planVersion: 0)
        assertNotNull(plan4.save())
        // let's create some processes
        Process process1 = new Process(finished: true, jobExecutionPlan: plan1, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process2 = new Process(finished: true, jobExecutionPlan: plan2, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process3 = new Process(finished: true, jobExecutionPlan: plan3, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        Process process4 = new Process(finished: true, jobExecutionPlan: plan4, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process1.save())
        assertNotNull(process2.save())
        assertNotNull(process3.save())
        assertNotNull(process4.save())
        // for none of the plans there should be a failed
        assertNull(service.getLastFailedProcess(plan1))
        assertNull(service.getLastFailedProcess(plan2))
        assertNull(service.getLastFailedProcess(plan3))
        assertNull(service.getLastFailedProcess(plan4))
        // now create some Processing Steps
        JobDefinition jobDefinition1 = new JobDefinition(name: "test", bean: "foo", plan: plan1)
        assertNotNull(jobDefinition1.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "test", bean: "foo", plan: plan2)
        assertNotNull(jobDefinition2.save())
        JobDefinition jobDefinition3 = new JobDefinition(name: "test", bean: "foo", plan: plan3)
        assertNotNull(jobDefinition3.save())
        JobDefinition jobDefinition4 = new JobDefinition(name: "test", bean: "foo", plan: plan4)
        assertNotNull(jobDefinition4.save())
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition1)
        assertNotNull(step1.save())
        ProcessingStep step2 = new ProcessingStep(process: process2, jobDefinition: jobDefinition2)
        assertNotNull(step2.save())
        ProcessingStep step3 = new ProcessingStep(process: process3, jobDefinition: jobDefinition3)
        assertNotNull(step3.save())
        ProcessingStep step4 = new ProcessingStep(process: process4, jobDefinition: jobDefinition4)
        assertNotNull(step4.save())
        // now a created processing step update for each
        ProcessingStepUpdate created1 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step1)
        assertNotNull(created1.save())
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created1), processingStep: step2)
        assertNotNull(created2.save())
        ProcessingStepUpdate created3 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created2), processingStep: step3)
        assertNotNull(created3.save())
        ProcessingStepUpdate created4 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created3), processingStep: step4)
        assertNotNull(created4.save())
        // there should still be nothing
        assertNull(service.getLastFailedProcess(plan1))
        assertNull(service.getLastFailedProcess(plan2))
        assertNull(service.getLastFailedProcess(plan3))
        assertNull(service.getLastFailedProcess(plan4))
        // create a failure for ProcessingStep 4
        ProcessingStepUpdate failure4 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(created4), processingStep: step4, previous: created4)
        assertNotNull(failure4.save())
        // should render process4 for plan4
        assertNull(service.getLastFailedProcess(plan1))
        assertNull(service.getLastFailedProcess(plan2))
        assertNull(service.getLastFailedProcess(plan3))
        assertSame(process4, service.getLastFailedProcess(plan4))
        // now create a failure for ProcessingStep 3
        ProcessingStepUpdate failure3 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(failure4), processingStep: step3, previous: created3)
        assertNotNull(failure3.save())
        assertNull(service.getLastFailedProcess(plan1))
        assertNull(service.getLastFailedProcess(plan2))
        assertSame(process3, service.getLastFailedProcess(plan3))
        assertSame(process4, service.getLastFailedProcess(plan4))
        // now one for ProcessingStep1
        ProcessingStepUpdate failure1 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(failure3), processingStep: step1, previous: created1)
        assertNotNull(failure1.save())
        assertSame(process1, service.getLastFailedProcess(plan1))
        assertSame(process1, service.getLastFailedProcess(plan2))
        assertSame(process1, service.getLastFailedProcess(plan3))
        assertSame(process4, service.getLastFailedProcess(plan4))
        // last but not least for ProcessingStep2
        ProcessingStepUpdate failure2 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(failure1), processingStep: step2, previous: created2)
        assertNotNull(failure2.save())
        assertSame(process1, service.getLastFailedProcess(plan1))
        assertSame(process2, service.getLastFailedProcess(plan2))
        assertSame(process2, service.getLastFailedProcess(plan3))
        assertSame(process4, service.getLastFailedProcess(plan4))
    }

    @Test
    public void testGetLastFinishedProcess() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        assertNotNull(plan.save())
        // there should not be a finished Process
        assertNull(service.getLastFinishedProcess(plan))
        Process process1 = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process1.save())
        Process process2 = new Process(finished: true, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process2.save())
        assertNull(service.getLastFinishedProcess(plan))
        // now create some Processing Steps
        JobDefinition jobDefinition1 = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition1.save())
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition1)
        assertNotNull(step1.save())
        ProcessingStep step2 = new ProcessingStep(process: process2, jobDefinition: jobDefinition1)
        assertNotNull(step2.save())
        assertNull(service.getLastFinishedProcess(plan))
        // now a created processing step update for each
        ProcessingStepUpdate created1 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step1)
        assertNotNull(created1.save())
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(created1), processingStep: step2)
        assertNotNull(created2.save())
        assertNull(service.getLastFinishedProcess(plan))
        // let's create a failure for process1
        ProcessingStepUpdate failure1 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(created2), processingStep: step1, previous: created1)
        assertNotNull(failure1.save())
        // process1 failed, but none succeeded
        assertSame(process1, service.getLastFailedProcess(plan))
        assertSame(process1, service.getLastFinishedProcess(plan))
        assertNull(service.getLastSucceededProcess(plan))
        // let's create a success event for the second process
        ProcessingStepUpdate success1 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(failure1), processingStep: step2, previous: created2)
        assertNotNull(success1.save())
        // all methods should return something useful
        assertSame(process1, service.getLastFailedProcess(plan))
        assertSame(process2, service.getLastFinishedProcess(plan))
        assertSame(process2, service.getLastSucceededProcess(plan))

        // we tested with first a failure than a success, no let's turn it around
        // create second processing step
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "foo", plan: plan, previous: jobDefinition1)
        assertNotNull(jobDefinition2.save())
        ProcessingStep step3 = new ProcessingStep(process: process1, jobDefinition: jobDefinition2, previous: step1)
        assertNotNull(step3.save())
        step1.next = step3
        assertNotNull(step1.save())
        // now we have a succeeded, but no failed
        assertSame(process2, service.getLastFinishedProcess(plan))
        assertSame(process2, service.getLastSucceededProcess(plan))
        assertNull(service.getLastFailedProcess(plan))
        ProcessingStepUpdate created3 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(success1), processingStep: step3)
        assertNotNull(created3.save())
        // nothing should have changed
        assertSame(process2, service.getLastFinishedProcess(plan))
        assertSame(process2, service.getLastSucceededProcess(plan))
        assertNull(service.getLastFailedProcess(plan))
        ProcessingStepUpdate success2 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(created3), processingStep: step3, previous: created3)
        assertNotNull(success2.save())
        // now process1 should be last succeeded
        assertSame(process1, service.getLastFinishedProcess(plan))
        assertSame(process1, service.getLastSucceededProcess(plan))
        assertNull(service.getLastFailedProcess(plan))
        // let's make Process2 fail
        ProcessingStep step4 = new ProcessingStep(process: process2, jobDefinition: jobDefinition2, previous: step2)
        assertNotNull(step4.save())
        step2.next = step4
        assertNotNull(step2.save())
        ProcessingStepUpdate created4 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(success2), processingStep: step4)
        assertNotNull(created4.save())
        ProcessingStepUpdate failure2 = new ProcessingStepUpdate(state: ExecutionState.FAILURE, date: nextDate(created4), processingStep: step4, previous: created4)
        assertNotNull(failure2.save())
        // now process1 should be last succeeded, process2 should be last failed, also in general
        assertSame(process2, service.getLastFinishedProcess(plan))
        assertSame(process1, service.getLastSucceededProcess(plan))
        assertSame(process2, service.getLastFailedProcess(plan))
    }

    @Test
    public void testGetLastExecutedProcess() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        assertNotNull(plan.save())
        // there should not be a started Process
        assertNull(service.getLastExecutedProcess(plan))
        // create the process
        Process process1 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process1.save())
        // there still is no started process
        assertNull(service.getLastExecutedProcess(plan))
        // create the Processing Step
        JobDefinition jobDefinition1 = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition1.save())
        plan.firstJob = jobDefinition1
        assertNotNull(plan.save())
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition1)
        assertNotNull(step1.save())
        // there still is no started Process
        assertNull(service.getLastExecutedProcess(plan))
        // now a created Processing Step Update...
        ProcessingStepUpdate created1 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: new Date(), processingStep: step1)
        assertNotNull(created1.save())
        // ... which changes the world
        assertSame(process1, service.getLastExecutedProcess(plan))
        // adding a success should not change anything
        ProcessingStepUpdate success1 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(created1), processingStep: step1, previous: created1)
        assertNotNull(success1.save())
        assertSame(process1, service.getLastExecutedProcess(plan))
        // but adding a second process will change
        Process process2 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process2.save())
        ProcessingStep step2 = new ProcessingStep(process: process2, jobDefinition: jobDefinition1)
        assertNotNull(step2.save())
        ProcessingStepUpdate created2 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(success1), processingStep: step2)
        assertNotNull(created2.save())
        // now it should be Process2
        assertSame(process2, service.getLastExecutedProcess(plan))
        // adding a success should not change anything
        ProcessingStepUpdate success2 = new ProcessingStepUpdate(state: ExecutionState.SUCCESS, date: nextDate(created2), processingStep: step2, previous: created2)
        assertNotNull(success2.save())
        assertSame(process2, service.getLastExecutedProcess(plan))
        // even adding a new Processing Step to Process 1 should not change anything
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "foo", plan: plan, previous: jobDefinition1)
        assertNotNull(jobDefinition2.save())
        jobDefinition1.next = jobDefinition2
        assertNotNull(jobDefinition1.save())
        ProcessingStep step3 = new ProcessingStep(process: process1, jobDefinition: jobDefinition2, previous: step1)
        assertNotNull(step3.save())
        ProcessingStepUpdate created3 = new ProcessingStepUpdate(state: ExecutionState.CREATED, date: nextDate(success2), processingStep: step3)
        assertNotNull(created3.save())
        assertSame(process2, service.getLastExecutedProcess(plan))
    }

    public void testGetLatestUpdatesForPlanInStateFailure() {
        JobExecutionPlanService service = new JobExecutionPlanService()
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        assertNotNull(plan.save())
        def planData = service.getLatestUpdatesForPlan(plan)
        assertTrue(planData.isEmpty())
        // create the process
        Process process1 = new Process(finished: false, jobExecutionPlan: plan, started: new Date(), startJobClass: "foo", startJobVersion: "1")
        assertNotNull(process1.save())
        // create the Processing Step
        JobDefinition jobDefinition1 = new JobDefinition(name: "test", bean: "foo", plan: plan)
        assertNotNull(jobDefinition1.save())
        plan.firstJob = jobDefinition1
        assertNotNull(plan.save())
        ProcessingStep step1 = new ProcessingStep(process: process1, jobDefinition: jobDefinition1)
        assertNotNull(step1.save())
        // now a created Processing Step Update...
        mockProcessingStepAsFinished(step1)
        // latest updates for plan should show one
        planData = service.getLatestUpdatesForPlan(plan)
        assertFalse(planData.isEmpty())
        assertTrue(planData.containsKey(process1))
        assertEquals(planData.get(process1).state, ExecutionState.FINISHED)
        // lets restrict the returned data
        assertEquals(1, service.getNumberOfProcesses(plan,  ExecutionState.FINISHED))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, ExecutionState.FINISHED)
        assertFalse(planData.isEmpty())
        assertTrue(planData.containsKey(process1))
        assertEquals(planData.get(process1).state, ExecutionState.FINISHED)
        // try to restrict on something else
        assertEquals(0, service.getNumberOfProcesses(plan,  ExecutionState.CREATED))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, ExecutionState.CREATED)
        assertTrue(planData.isEmpty())
        // lets create a failure for the processing step
        ProcessingStepUpdate update = ProcessingStepUpdate.findByState(ExecutionState.FINISHED)
        ProcessingStepUpdate failure = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FAILURE,
            previous: update,
            processingStep: update.processingStep
        )
        assertNotNull(failure.save())
        // test for failure
        assertEquals(1, service.getNumberOfProcesses(plan,  ExecutionState.FAILURE))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, ExecutionState.FAILURE)
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
        assertNotNull(restarted.save())
        // test for failure
        assertEquals(0, service.getNumberOfProcesses(plan,  ExecutionState.FAILURE))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, ExecutionState.FAILURE)
        assertTrue(planData.isEmpty())
        // test for restarted
        assertEquals(1, service.getNumberOfProcesses(plan,  ExecutionState.RESTARTED))
        planData = service.getLatestUpdatesForPlan(plan, 10, 0, "id", false, ExecutionState.RESTARTED)
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
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        assertFalse(plan.enabled)
        // let's enable the Plan
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.enablePlan(plan)
        }
        assertTrue(plan.enabled)
        // enabling the plan again should not change anything
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.enablePlan(plan)
        }
        assertTrue(plan.enabled)
    }

    @Test
    void testEnablePlanForStartJob() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0)
        plan = plan.save(flush: true)
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save())
        plan.startJob = startJob
        assertNotNull(plan.save(flush: true))

        // get a startJob Instance for the testStartJob and inject the JobExecutionPlan
        TestSingletonStartJob job = null
        StartJobDefinition.withNewSession {
            job = grailsApplication.mainContext.getBean("testSingletonStartJob") as TestSingletonStartJob
            job.setExecutionPlan(plan)
        }
        assertNotNull(job)
        // everything should be disabled now
        assertFalse(plan.enabled)
        assertFalse(job.executionPlan.enabled)
        // let's enable the Plan
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.enablePlan(plan)
        }
        assertTrue(plan.enabled)
        assertTrue(job.executionPlan.enabled)
    }

    @Test
    void testDisablePlan() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        assertTrue(plan.enabled)
        // let's disable the Plan
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.disablePlan(plan)
        }
        assertFalse(plan.enabled)
        // disabling the plan again should not change anything
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.disablePlan(plan)
        }
        assertFalse(plan.enabled)
    }

    @Test
    void testDisablePlanForStartJob() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "testGetLastFinishedProcess", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save())
        plan.startJob = startJob
        assertNotNull(plan.save(flush: true))

        // get a startJob Instance for the testStartJob and inject the JobExecutionPlan
        TestSingletonStartJob job = null
        StartJobDefinition.withNewSession {
            job = grailsApplication.mainContext.getBean("testSingletonStartJob") as TestSingletonStartJob
            job.setExecutionPlan(plan)
        }
        assertNotNull(job)
        // everything should be enabled now
        assertTrue(plan.enabled)
        assertTrue(job.executionPlan.enabled)
        // let's disable the Plan
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.disablePlan(plan)
        }
        assertFalse(plan.enabled)
        assertFalse(job.getExecutionPlan().enabled)
    }

    @Test
    void testGetPlanSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // as a user it should fail
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getPlan(plan.id)
            }
        }
        // an operator should see it
        SpringSecurityUtils.doWithAuth("operator") {
            assertSame(plan, jobExecutionPlanService.getPlan(plan.id))
        }
        SpringSecurityUtils.doWithAuth("admin") {
            assertSame(plan, jobExecutionPlanService.getPlan(plan.id))
            // grant read priv to testuser
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now the testuser should be able to see it
        SpringSecurityUtils.doWithAuth("testuser") {
            assertSame(plan, jobExecutionPlanService.getPlan(plan.id))
        }
        // but a different user should not be able to see it
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getPlan(plan.id)
            }
        }
    }

    void testEnablePlanSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // a user should not be able to enable a plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.enablePlan(plan)
            }
        }
        // an operator should be able to enable the plan
        SpringSecurityUtils.doWithAuth("operator") {
            jobExecutionPlanService.enablePlan(plan)
        }
        // an admin user should be able to enable the plan
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.enablePlan(plan)
            // let's give read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // which actually should not change anything as it requires write permission
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.enablePlan(plan)
            }
        }
        SpringSecurityUtils.doWithAuth("admin") {
            // so let's add write
            aclUtilService.addPermission(plan, "testuser", BasePermission.WRITE)
        }
        SpringSecurityUtils.doWithAuth("testuser") {
            jobExecutionPlanService.enablePlan(plan)
        }
        // but another user should not see it
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.enablePlan(plan)
            }
        }
    }

    void testDisablePlanSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // a user should not be able to disable a plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.disablePlan(plan)
            }
        }
        // an operator should be able to disable the plan
        SpringSecurityUtils.doWithAuth("operator") {
            jobExecutionPlanService.disablePlan(plan)
        }
        // an admin user should be able to disable the plan
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.disablePlan(plan)
            // let's give read permission to the user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // which actually should not change anything as it requires write permission
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.disablePlan(plan)
            }
        }
        SpringSecurityUtils.doWithAuth("admin") {
            // so let's add write
            aclUtilService.addPermission(plan, "testuser", BasePermission.WRITE)
        }
        SpringSecurityUtils.doWithAuth("testuser") {
            jobExecutionPlanService.disablePlan(plan)
        }
        // but another user should not be allowed to disable the plan
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.disablePlan(plan)
            }
        }
    }

    @Test
    void testGetAllPlansSecurity() {
        // create three plans
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        JobExecutionPlan plan1 = new JobExecutionPlan(name: "test1", obsoleted: false, planVersion: 0, enabled: true)
        plan1 = plan1.save(flush: true)
        assertNotNull(plan1)
        JobExecutionPlan plan2 = new JobExecutionPlan(name: "test2", obsoleted: false, planVersion: 0, enabled: true)
        plan2 = plan2.save(flush: true)
        assertNotNull(plan2)
        // user should get an empty list
        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(jobExecutionPlanService.getAllJobExecutionPlans().empty)
        }
        // an operator should see all three plans
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(3, jobExecutionPlanService.getAllJobExecutionPlans().size())
        }
        // admin should see all three plans
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(3, jobExecutionPlanService.getAllJobExecutionPlans().size())
            // grant read to testuser
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now testuser should be able to see them
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(1, jobExecutionPlanService.getAllJobExecutionPlans().size())
            assertSame(plan, jobExecutionPlanService.getAllJobExecutionPlans().first())
        }
        // give grant for other plans
        SpringSecurityUtils.doWithAuth("admin") {
            // grant read to testuser
            aclUtilService.addPermission(plan1, "testuser", BasePermission.READ)
            aclUtilService.addPermission(plan2, "testuser", BasePermission.READ)
        }
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(3, jobExecutionPlanService.getAllJobExecutionPlans().size())
            assertSame(plan2, jobExecutionPlanService.getAllJobExecutionPlans().first())
            assertSame(plan, jobExecutionPlanService.getAllJobExecutionPlans().last())
        }
        // but other user should not see anything
        SpringSecurityUtils.doWithAuth("user") {
            assertTrue(jobExecutionPlanService.getAllJobExecutionPlans().empty)
        }
    }

    @Test
    void testGetAllProcessesSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to get processes for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getAllProcesses(plan)
            }
        }
        // operator should be able to access the plans
        SpringSecurityUtils.doWithAuth("operator") {
            assertTrue(jobExecutionPlanService.getAllProcesses(plan).empty)
        }
        // admin should be able to do so
        SpringSecurityUtils.doWithAuth("admin") {
            assertTrue(jobExecutionPlanService.getAllProcesses(plan).empty)
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should be able to get the processes
        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(jobExecutionPlanService.getAllProcesses(plan).empty)
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getAllProcesses(plan)
            }
        }
    }

    @Test
    void testGetLatestUpdatesForPlanSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to get the latest updates for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLatestUpdatesForPlan(plan)
            }
        }
        // operator should be able to get the latest updates for the plan
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(jobExecutionPlanService.getLatestUpdatesForPlan(plan).empty)
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(jobExecutionPlanService.getLatestUpdatesForPlan(plan).empty)
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also get the updates
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(jobExecutionPlanService.getLatestUpdatesForPlan(plan).empty)
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLatestUpdatesForPlan(plan)
            }
        }
    }

    @Test
    void testIsProcessRunningSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to get the information about running processes for this plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.isProcessRunning(plan)
            }
        }
        // operator should be able to get information about running processes
        SpringSecurityUtils.doWithAuth("operator") {
            assertFalse(jobExecutionPlanService.isProcessRunning(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertFalse(jobExecutionPlanService.isProcessRunning(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should be able to see whether a process is running
        SpringSecurityUtils.doWithAuth("testuser") {
            assertFalse(jobExecutionPlanService.isProcessRunning(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.isProcessRunning(plan)
            }
        }
    }

    @Test
    void testGetProcessCountSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to retrieve the Process count for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getProcessCount(plan)
            }
        }
        // an operator should be able to retrieve the Process count
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(0, jobExecutionPlanService.getProcessCount(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(0, jobExecutionPlanService.getProcessCount(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(0, jobExecutionPlanService.getProcessCount(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getProcessCount(plan)
            }
        }
    }

    @Test
    void testGetLastSucceddedProcessSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to retrieve the last succeeded Process for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastSucceededProcess(plan)
            }
        }
        // an operator should be able tor retrieve the last succeeded Process
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(jobExecutionPlanService.getLastSucceededProcess(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(jobExecutionPlanService.getLastSucceededProcess(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(jobExecutionPlanService.getLastSucceededProcess(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastSucceededProcess(plan)
            }
        }
    }

    @Test
    void testGetLastFailedProcessSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to retrieve the last failed Process for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastFailedProcess(plan)
            }
        }
        // an operator should be able to retrieve the last failed Process
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(jobExecutionPlanService.getLastFailedProcess(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(jobExecutionPlanService.getLastFailedProcess(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(jobExecutionPlanService.getLastFailedProcess(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastFailedProcess(plan)
            }
        }
    }

    @Test
    void testGetLastFinishedProcessSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to retrieve the last finished Process for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastFinishedProcess(plan)
            }
        }
        // an operator should be able to retrieve the last finished Process
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(jobExecutionPlanService.getLastFinishedProcess(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(jobExecutionPlanService.getLastFinishedProcess(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(jobExecutionPlanService.getLastFinishedProcess(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastFinishedProcess(plan)
            }
        }
    }

    @Test
    void testGetLastExecutedProcessSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to retrieve the last executed Process for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastExecutedProcess(plan)
            }
        }
        // an operator should be able to retrieve the last executed Process
        SpringSecurityUtils.doWithAuth("operator") {
            assertNull(jobExecutionPlanService.getLastExecutedProcess(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertNull(jobExecutionPlanService.getLastExecutedProcess(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            assertNull(jobExecutionPlanService.getLastExecutedProcess(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getLastExecutedProcess(plan)
            }
        }
    }

    @Test
    void testGetNumberOfProcessesSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to retrieve the number of Processes for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getNumberOfProcesses(plan)
            }
        }
        // an operator should be able to retrieve the number of Processes
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(0, jobExecutionPlanService.getNumberOfProcesses(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(0, jobExecutionPlanService.getNumberOfProcesses(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(0, jobExecutionPlanService.getNumberOfProcesses(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getNumberOfProcesses(plan)
            }
        }
    }

    @Test
    void testGetNumberOfSuccessfulFinishedProcessesSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to retrieve the number of successfully finished Processes for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getNumberOfSuccessfulFinishedProcesses(plan)
            }
        }
        // an operator should be able to retrieve the number of successfully finished Processes
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(0, jobExecutionPlanService.getNumberOfSuccessfulFinishedProcesses(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(0, jobExecutionPlanService.getNumberOfSuccessfulFinishedProcesses(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(0, jobExecutionPlanService.getNumberOfSuccessfulFinishedProcesses(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getNumberOfSuccessfulFinishedProcesses(plan)
            }
        }
    }

    @Test
    void testGetNumberOfFinishedProcessesSecurity() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        // user should not be able to retrieve the number of finished Processes for the plan
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getNumberOfFinishedProcesses(plan)
            }
        }
        // an operator should be able to retrieve the number of finished Processes
        SpringSecurityUtils.doWithAuth("operator") {
            assertEquals(0, jobExecutionPlanService.getNumberOfFinishedProcesses(plan))
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            assertEquals(0, jobExecutionPlanService.getNumberOfFinishedProcesses(plan))
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            assertEquals(0, jobExecutionPlanService.getNumberOfFinishedProcesses(plan))
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.getNumberOfFinishedProcesses(plan)
            }
        }
    }

    @Test
    void testPlanInformation() {
        JobExecutionPlan plan = new JobExecutionPlan(name: "test", obsoleted: false, planVersion: 0, enabled: true)
        plan = plan.save(flush: true)
        assertNotNull(plan)
        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: plan)
        assertNotNull(startJob.save())
        plan.startJob = startJob
        assertNotNull(plan.save(flush: true))
        // user should not be able to retrieve the plan information
        SpringSecurityUtils.doWithAuth("testuser") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.planInformation(plan)
            }
        }
        // an operator should be able to retrieve the plan information
        SpringSecurityUtils.doWithAuth("operator") {
            jobExecutionPlanService.planInformation(plan)
        }
        // but admin should
        SpringSecurityUtils.doWithAuth("admin") {
            jobExecutionPlanService.planInformation(plan)
            // grant read to user
            aclUtilService.addPermission(plan, "testuser", BasePermission.READ)
        }
        // now user should also be able to retrieve the information
        SpringSecurityUtils.doWithAuth("testuser") {
            jobExecutionPlanService.planInformation(plan)
        }
        // but a different user should not
        SpringSecurityUtils.doWithAuth("user") {
            shouldFail(AccessDeniedException) {
                jobExecutionPlanService.planInformation(plan)
            }
        }
    }

    private Date nextDate(ProcessingStepUpdate update) {
        return new Date(update.date.time + 1)
    }

    private ProcessingStepUpdate mockProcessingStepAsFinished(ProcessingStep step) {
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

    private void mockProcessingStepAsFailed(ProcessingStep step) {
        ProcessingStepUpdate update = mockProcessingStepAsFinished(step)
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.FAILURE,
            previous: update,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
    }

    private void mockProcessingStepAsSucceeded(ProcessingStep step) {
        ProcessingStepUpdate update = mockProcessingStepAsFinished(step)
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.SUCCESS,
            previous: update,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
    }
}
