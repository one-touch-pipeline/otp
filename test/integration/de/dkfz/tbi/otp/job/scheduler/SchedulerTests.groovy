package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.JobMailService
import de.dkfz.tbi.otp.utils.MailHelperService

import static org.junit.Assert.*
import java.util.concurrent.ExecutorService

import org.apache.commons.logging.impl.NoOpLog

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.EndStateAwareJob
import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.job.processing.InvalidStateException
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import de.dkfz.tbi.otp.ngsdata.Realm
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.junit.*

class SchedulerTests extends AbstractIntegrationTest {

    GrailsApplication grailsApplication
    PbsMonitorService pbsMonitorService
    Scheduler scheduler
    SchedulerService schedulerService

    @After
    void tearDown() {
        TestCase.removeMetaClass(JobMailService, scheduler.jobMailService)
    }

    @Test
    void testNormalJobExecution() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job job = grailsApplication.mainContext.getBean("testEndStateAwareJob", step, [] as Set) as Job
        job.log = new NoOpLog()
        // There is no Created ProcessingStep update - execution should fail
        shouldFail(RuntimeException) {
            scheduler.executeJob(job)
        }
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        shouldFail(InvalidStateException) {
            job.getOutputParameters()
        }
        scheduler.executeJob(job)
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
        // and there should be some output parameters
        List<Parameter> params = step.output.toList().sort { it.type.name }
        assertEquals(2, params.size())
        assertEquals("test", params[0].type.name)
        assertEquals("1234", params[0].value)
        assertNull(params[0].type.className)
        assertSame(jobDefinition, params[0].type.jobDefinition)
        assertEquals("test2", params[1].type.name)
        assertEquals("4321", params[1].value)
        assertNull(params[1].type.className)
        assertSame(jobDefinition, params[1].type.jobDefinition)
    }

    @Test
    void testNormalEndStateAwareJobExecution() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "testEndStateAware", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestEndStateAwareJob("testEndStateAware", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job endStateAwareJob = grailsApplication.mainContext.getBean("testEndStateAwareJob", step, [] as Set) as Job
        endStateAwareJob.log = new NoOpLog()
        // There is no Created ProcessingStep update - execution should fail
        shouldFail(RuntimeException) {
            scheduler.executeJob(endStateAwareJob)
        }
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        shouldFail(InvalidStateException) {
            endStateAwareJob.getOutputParameters()
        }
        shouldFail(InvalidStateException) {
            endStateAwareJob.getEndState()
        }
        scheduler.executeJob(endStateAwareJob)
        assertEquals(ExecutionState.SUCCESS, endStateAwareJob.endState)
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
        // and there should be some output parameters
        List<Parameter> params = step.output.toList().sort { it.type.name }
        assertEquals(2, params.size())
        assertEquals("test", params[0].type.name)
        assertEquals("1234", params[0].value)
        assertNull(params[0].type.className)
        assertSame(jobDefinition, params[0].type.jobDefinition)
        assertEquals("test2", params[1].type.name)
        assertEquals("4321", params[1].value)
        assertNull(params[1].type.className)
        assertSame(jobDefinition, params[1].type.jobDefinition)
    }

    @Test
    void testFailingEndStateAwareJobExecution() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "testFailureEndStateAware", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestEndStateAwareJob("testEndStateAware", jep, null, "testFailureEndStateAwareJob")
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        Job endStateAwareJob = grailsApplication.mainContext.getBean("testFailureEndStateAwareJob", step, [] as Set) as Job
        endStateAwareJob.log = new NoOpLog()
        // There is no Created ProcessingStep update - execution should fail
        int executedCounter = 0
        scheduler.jobMailService.metaClass.sendErrorNotificationIfFastTrack = { ProcessingStep step2, String message ->
            executedCounter++
        }
        shouldFail(RuntimeException) {
            scheduler.executeJob(endStateAwareJob)
        }
        assert 1 == executedCounter
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        shouldFail(InvalidStateException) {
            endStateAwareJob.getOutputParameters()
        }
        shouldFail(InvalidStateException) {
            endStateAwareJob.getEndState()
        }
        scheduler.executeJob(endStateAwareJob)
        assertEquals(ExecutionState.FAILURE, endStateAwareJob.endState)
        step.refresh()
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertTrue(process.finished)
        assert 2 == executedCounter
    }

    @Test
    void testFailingExecution() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "failingTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(process: process, jobDefinition: jobDefinition)
        assertNotNull(step.save())
        Job job = grailsApplication.mainContext.getBean("failingTestJob", step, [] as Set) as Job
        job.log = new NoOpLog()
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        boolean notified = false
        scheduler.jobMailService.metaClass.sendErrorNotificationIfFastTrack = { ProcessingStep step2, Throwable exceptionToBeHandled ->
            if (notified) {
                assert false: 'called twiced'
            } else {
                notified = true
            }
        }

        shouldFail(Exception) {
            scheduler.executeJob(job)
        }
        // now we should have three processingStepUpdates for the processing step
        step.refresh()
        assertEquals(3, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FAILURE, updates[2].state)
        assertNotNull(updates[2].error)
        assertEquals("Testing", updates[2].error.errorMessage)
        assert notified
    }

    @Test
    void testMissingOutputParameter() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // create a third parameter type for which the job does not create a parameter
        ParameterType type = new ParameterType(name: "fail", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(type.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        // run the Job
        Job job = grailsApplication.mainContext.getBean("testJob", step, [] as Set) as Job
        job.log = new NoOpLog()
        scheduler.executeJob(job)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("Required Output Parameter of type ${type.id} is not set.".toString(), updates[3].error.errorMessage)
    }

    @Test
    void testInputAsOutputParameter() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "directTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = new JobDefinition(name: "test2", bean: "directTestJob", plan: jep, previous: jobDefinition)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        ParameterType test = new ParameterType(name: "test", description: "Test description", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)
        assertNotNull(test.save())
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        Job job = grailsApplication.mainContext.getBean("directTestJob", step, [] as Set) as Job
        job.log = new NoOpLog()
        // run the Job
        scheduler.executeJob(job)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("Parameter abcd is either not defined for JobDefintion ${jobDefinition.id} or not of type Output.".toString(), updates[3].error.errorMessage)
        // verify that the test works if we use a proper parameter type
        test.parameterUsage = ParameterUsage.OUTPUT
        assertNotNull(test.save())
        process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        job = grailsApplication.mainContext.getBean("directTestJob", step, [] as Set) as Job
        job.log = new NoOpLog()
        // run the Job
        scheduler.executeJob(job)
        assertEquals(3, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
    }

    @Test
    void testMissingPbsIds() {
        // this test checks that PbsJobs set the PbsIds
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "failingPbsTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        Job job = grailsApplication.mainContext.getBean("failingPbsTestJob", step, [] as Set) as Job
        job.log = new NoOpLog()
        // run the Job
        scheduler.executeJob(job)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("PbsJob does not provide PBS Process Ids", updates[3].error.errorMessage)
    }

    @Test
    void testMissingPbsOutputParameterType() {
        // this test checks that PbsJobs set the PbsIds
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "pbsTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        Job job = grailsApplication.mainContext.getBean("pbsTestJob", step, [] as Set) as Job
        job.log = new NoOpLog()
        // run the Job
        scheduler.executeJob(job)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("PbsJob does not have required output parameter type", updates[3].error.errorMessage)
    }

    @Ignore
    @Test
    void testMissingPbsRealm() {
        // this test checks that PbsJobs set the PbsIds
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "pbsTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // required output parameter
        ParameterType pbsOutputParameterType = new ParameterType(name: "__pbsIds", description: "Ids on PBS", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(pbsOutputParameterType.save())
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        Job job = grailsApplication.mainContext.getBean("pbsTestJob", step, [] as Set) as Job
        // run the Job
        scheduler.executeJob(job)
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("PbsJob does not provide the Realm it is operating on or Realm Id is incorrect", updates[3].error.errorMessage)

        // Create the Realm
        Realm realm = new Realm(name: "realm", rootPath: "/", webHost: "http://localhost", host: "localhost", port: 1234, unixUser: "test", timeout: 200, pbsOptions: "")
        assertNotNull(realm.save())
        // Create a new Process
        Process process2 = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process2.save())
        step = new ProcessingStep(jobDefinition: jobDefinition, process: process2)
        assertNotNull(step.save())
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        job = grailsApplication.mainContext.getBean("pbsTestJob", step, [] as Set) as Job
        // use wrong realm
        job.setRealm(realm.id + 100)
        // run the Job
        scheduler.executeJob(job)
        // should fail due to missing ParameterType
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("PbsJob does not provide the Realm it is operating on or Realm Id is incorrect", updates[3].error.errorMessage)

        // Create a new Process
        Process process3 = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process3.save())
        step = new ProcessingStep(jobDefinition: jobDefinition, process: process3)
        assertNotNull(step.save())
        update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        job = grailsApplication.mainContext.getBean("pbsTestJob", step, [] as Set) as Job
        job.setRealm(realm.id)
        // run the Job
        scheduler.executeJob(job)
        // should fail due to missing ParameterType
        assertEquals(4, ProcessingStepUpdate.countByProcessingStep(step))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.FAILURE, updates[3].state)
        assertNotNull(updates[3].error)
        assertEquals("PbsJob does not have required output parameter type for pbs realm", updates[3].error.errorMessage)
    }

    @Ignore
    @Test
    void testPbsParameters() {
        JobExecutionPlan jep = new JobExecutionPlan(name: "test", planVersion: 0, startJobBean: "someBean")
        assertNotNull(jep.save())
        JobDefinition jobDefinition = new JobDefinition(name: "test", bean: "pbsTestJob", plan: jep)
        assertNotNull(jobDefinition.save())
        JobDefinition jobDefinition2 = createTestEndStateAwareJob("testEndStateAware", jep)
        assertNotNull(jobDefinition2.save())
        jobDefinition.next = jobDefinition2
        assertNotNull(jobDefinition.save())
        // TODO: add Watchdog job definition
        jep.firstJob = jobDefinition
        assertNotNull(jep.save())
        // required output parameter
        ParameterType pbsOutputParameterType = new ParameterType(name: "__pbsIds", description: "Ids on PBS", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(pbsOutputParameterType.save())
        // create the Realm
        Realm realm = new Realm(name: "realm", rootPath: "/", webHost: "http://localhost", host: "localhost", port: 1234, unixUser: "test", timeout: 200, pbsOptions: "")
        assertNotNull(realm.save())
        ParameterType pbsRealmOutputParameterType = new ParameterType(name: "__pbsRealm", description: "PBS Realm", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.OUTPUT)
        assertNotNull(pbsRealmOutputParameterType.save())
        // Create the Process
        Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "de.dkfz.tbi.otp.job.scheduler.SchedulerTests", startJobVersion: "1")
        assertNotNull(process.save())
        ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process)
        assertNotNull(step.save())
        ProcessingStepUpdate update = new ProcessingStepUpdate(
            date: new Date(),
            state: ExecutionState.CREATED,
            previous: null,
            processingStep: step
            )
        assertNotNull(update.save(flush: true))
        Job job = grailsApplication.mainContext.getBean("pbsTestJob", step, [] as Set) as Job
        job.setRealm(realm.id)
        // run the Job
        scheduler.executeJob(job)
        assertEquals(3, ProcessingStepUpdate.countByProcessingStep(step))
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        // and there should be some output parameters
        List<Parameter> params = step.output.toList().sort { it.type.name }
        assertEquals(2, params.size())
        assertEquals("__pbsIds", params[0].type.name)
        assertEquals("1,2,3", params[0].value)
        assertEquals("__pbsRealm", params[1].type.name)
        assertEquals("${realm.id}".toString(), params[1].value)
    }

    static void assertFailed(final Job job, final String expectedErrorMessage, final boolean twice = false) {
        final ProcessingStep step = job.processingStep
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assert updates.size() == twice ? 4 : 3
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FAILURE, updates[2].state)
        assertNotNull(updates[2].error)
        assert updates[2].error.errorMessage.contains(expectedErrorMessage)
        if (twice) {
            // We get the failure twice because the exception is caught and handled on the other thread and then rethrown
            // (from the Future.get() method), such that it is thrown from the job's execute() method as well.
            assertEquals(ExecutionState.FAILURE, updates[3].state)
            assertNotNull(updates[3].error)
            assert updates[3].error.errorMessage.contains(expectedErrorMessage)
        }
        assertTrue(step.process.finished)
    }

    static void assertSucceeded(final EndStateAwareJob job) {
        assertEquals(ExecutionState.SUCCESS, job.endState)
        final ProcessingStep step = job.processingStep
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        assert updates.size() == 4
        assertEquals(ExecutionState.CREATED, updates[0].state)
        assertEquals(ExecutionState.STARTED, updates[1].state)
        assertEquals(ExecutionState.FINISHED, updates[2].state)
        assertEquals(ExecutionState.SUCCESS, updates[3].state)
        assertTrue(step.process.finished)
    }

    private void doWithTrueExecutorService(final Closure closure) {
        final ExecutorService originalExecutorService = scheduler.executorService
        scheduler.executorService = grailsApplication.mainContext.trueExecutorService
        try {
            closure()
        } finally {
            scheduler.executorService = originalExecutorService
        }
    }
}
