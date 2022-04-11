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
package de.dkfz.tbi.otp.job.scheduler

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.util.Holders
import org.junit.After
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.restarting.RestartCheckerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

@Rollback
@Integration
class SchedulerServiceIntegrationSpec extends Specification implements UserAndRoles {

    RestartCheckerService restartCheckerService
    SchedulerService schedulerService

    Scheduler scheduler

    void setupData() {
        createUserAndRoles()

        SessionUtils.metaClass.static.withNewSession = { Closure c -> c() }

        schedulerService.queue.clear()
        schedulerService.running.clear()
        schedulerService.schedulerActive = true
        schedulerService.startupOk = true
        schedulerService.metaClass.executeInNewThread = { Job job, Closure c ->
            c()
        }

        restartCheckerService.metaClass.canWorkflowBeRestarted = { ProcessingStep step -> false }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(RestartCheckerService, restartCheckerService)
        TestCase.removeMetaClass(SchedulerService, schedulerService)
        TestCase.removeMetaClass(SessionUtils)
    }

    void "testEndOfProcess"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()
        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)
        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process, jobClass: null)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        expect:
        !process.finished
        step.jobClass == null

        when:
        schedulerService.queue.add(step)
        schedulerService.schedule()

        then:
        process.finished
        step.jobClass
        TestEndStateAwareJob.class.name == step.jobClass
        areQueueAndRunningEmpty()
    }

    void "testCompleteProcess"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        jobDefinition.save(flush: true)
        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process, jobClass: null)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        expect:
        !process.finished
        step.jobClass == null
        step.next == null
        step.previous == null
        1 == ProcessingStep.countByProcess(process)

        when:
        schedulerService.queue.add(step)
        schedulerService.schedule()

        then:
        !process.finished
        step.jobClass
        de.dkfz.tbi.otp.job.jobs.TestJob.class.name == step.jobClass
        step.next
        step.previous == null
        step.id != step.next.id
        step == step.next.previous
        2 == ProcessingStep.countByProcess(process)

        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()
        3 == ProcessingStepUpdate.countByProcessingStep(step)

        when:
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }

        then:
        ExecutionState.CREATED == updates[0].state
        ExecutionState.STARTED == updates[1].state
        ExecutionState.FINISHED == updates[2].state

        when:
        schedulerService.schedule()
        process.refresh()
        //process = Process.get(process.id)

        then:
        process.finished
        areQueueAndRunningEmpty()
        2 == ProcessingStep.countByProcess(process)

        when:
        step = CollectionUtils.atMostOneElement(ProcessingStep.findAllByProcessAndPrevious(process, step))

        then:
        4 == ProcessingStepUpdate.countByProcessingStep(step)

        when:
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }

        then:
        ExecutionState.CREATED == updates[0].state
        ExecutionState.STARTED == updates[1].state
        ExecutionState.FINISHED == updates[2].state
        ExecutionState.SUCCESS == updates[3].state
    }

    void "testConstantParameterPassing"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        JobDefinition jobDefinition2 = createTestJob("test2", jep, jobDefinition)
        jobDefinition.next = jobDefinition2
        jobDefinition.save(flush: true)

        JobDefinition jobDefinition3 = createTestEndStateAwareJob("test3", jep, jobDefinition2)
        jobDefinition2.next = jobDefinition3
        jobDefinition2.save(flush: true)

        ParameterType constantParameterType = DomainFactory.createParameterType(jobDefinition: jobDefinition3, name: "constant")
        ParameterType constantParameterType2 = DomainFactory.createParameterType(jobDefinition: jobDefinition3, name: "constant2")
        jobDefinition3.addToConstantParameters(DomainFactory.createParameter(type: constantParameterType, value: "constant1"))
        jobDefinition3.addToConstantParameters(DomainFactory.createParameter(type: constantParameterType2, value: "constant2"))
        jobDefinition3.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        expect:
        areQueueAndRunningEmpty()

        when: "running the JobExecutionPlan"
        schedulerService.queue.add(step)
        schedulerService.schedule()

        then: "another Job should be be scheduled"
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when: "there should be a processing step for jobDefinition2"
        ProcessingStep step2 = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition2))

        then: "the processing step should have two input parameters generated by the testJob"
        step2
        step == step2.previous
        step2 == step.next
        step2.input.isEmpty()

        when: "continue"
        schedulerService.schedule()

        then: "the third Job should be scheduled"
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when: "there should be a processing step for jobDefinition3"
        ProcessingStep step3 = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition3))
        List<Parameter> parameters = step3.input.toList().sort { it.value }

        then: "the processing step should have two input parameters generated by the testJob"
        step3
        step2 == step3.previous
        step3 == step2.next
        step3.next == null

        then: "the processing step should have two input parameters generated by the testJob and two constant parameters"
        2 == parameters.size()
        constantParameterType == parameters[0].type
        constantParameterType2 == parameters[1].type
        "constant1" == parameters[0].value
        "constant2" == parameters[1].value
        1 == Parameter.countByType(constantParameterType)
        1 == Parameter.countByType(constantParameterType2)

        when: "continue"
        schedulerService.schedule()

        then:
        areQueueAndRunningEmpty()
        assertTrue(step3.process.finished)

        when: "run another process for same JobExecutionPlan, but trigger an exception by using an OUTPUT parameter as constant"
        constantParameterType.refresh()
        constantParameterType.parameterUsage = ParameterUsage.OUTPUT
        constantParameterType.save(flush: true)

        process = DomainFactory.createProcess(jobExecutionPlan: jep)
        step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        then:
        areQueueAndRunningEmpty()

        when: "running the JobExecutionPlan"
        schedulerService.queue.add(step)
        schedulerService.schedule()

        then: "another Job should be be scheduled"
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when:
        schedulerService.schedule()

        then: "mapping the parameters from the second Job should have failed"
        TestCase.shouldFail(SchedulerException) {
            schedulerService.schedule()
        }
        areQueueAndRunningEmpty()
        !process.finished

        when:
        ProcessingStep failedStep = ProcessingStep.findAllByJobDefinition(jobDefinition3).toList().sort { it.id }.last()
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(failedStep).sort { it.id }

        then: "the last JobDefinition should have a ProcessingStep with a created and a failed update"
        2 == updates.size()
        ExecutionState.CREATED == updates.first().state
        ExecutionState.FAILURE == updates.last().state
    }

    void "testParameterMapping"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        JobDefinition jobDefinition2 = createTestJob("test2", jep, jobDefinition)
        ParameterMapping mapping = new ParameterMapping(
                job: jobDefinition2,
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition, "test")),
                to: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition2, "input"))
        )
        jobDefinition2.addToParameterMappings(mapping)
        jobDefinition.next = jobDefinition2
        jobDefinition.save(flush: true)
        jobDefinition2.save(flush: true)
        jep.save(flush: true)

        JobDefinition jobDefinition3 = createTestEndStateAwareJob("test3", jep, jobDefinition2)
        mapping = new ParameterMapping(
                job: jobDefinition3,
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition2, "test")),
                to: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition3, "input"))
        )
        jobDefinition3.addToParameterMappings(mapping)
        mapping = new ParameterMapping(
                job: jobDefinition3,
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition2, "test2")),
                to: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition3, "input2"))
        )
        jobDefinition3.addToParameterMappings(mapping)
        jobDefinition2.next = jobDefinition3
        jobDefinition2.save(flush: true)
        jep.save(flush: true)

        ParameterType constantParameterType = DomainFactory.createParameterType(jobDefinition: jobDefinition3, name: "constant", parameterUsage: ParameterUsage.INPUT)
        ParameterType constantParameterType2 = DomainFactory.createParameterType(jobDefinition: jobDefinition3, name: "constant2", parameterUsage: ParameterUsage.INPUT)
        jobDefinition3.addToConstantParameters(DomainFactory.createParameter(type: constantParameterType, value: "constant1"))
        jobDefinition3.addToConstantParameters(DomainFactory.createParameter(type: constantParameterType2, value: "constant2"))
        jobDefinition3.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        expect:
        areQueueAndRunningEmpty()

        when: "running the JobExecutionPlan"
        schedulerService.queue.add(step)
        schedulerService.schedule()

        then: "another Job should be be scheduled"
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when:
        ProcessingStep step2 = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition2))

        then:
        step2
        step == step2.previous
        step2 == step.next
        step2.input
        1 == step2.input.size()
        "1234" == step2.input.toList().first().value

        when:
        schedulerService.schedule()

        ProcessingStep step3 = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition3))

        then:
        step3
        step2 == step3.previous
        step3.input
        List<Parameter> params = step3.input.toList().sort { it.value }
        4 == params.size()
        "1234" == params[0].value
        "4321" == params[1].value
        "constant1" == params[2].value
        "constant2" == params[3].value

        when:
        schedulerService.schedule()

        then:
        assertTrue(process.finished)
    }

    void "testPassthroughParameters"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        JobDefinition jobDefinition2 = createTestEndStateAwareJob("test2", jep, jobDefinition)
        ParameterType passThrough = DomainFactory.createParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.PASSTHROUGH)
        ParameterMapping mapping = new ParameterMapping(job: jobDefinition2, from: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition, "test")), to: passThrough)
        jobDefinition2.addToParameterMappings(mapping)
        jobDefinition.next = jobDefinition2
        jobDefinition.save(flush: true)
        jobDefinition2.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        expect:
        areQueueAndRunningEmpty()

        when:
        schedulerService.queue.add(step)
        CollectionUtils.atMostOneElement(Parameter.findAllByType(passThrough)) == null
        schedulerService.schedule()
        Parameter passThroughParameter = CollectionUtils.atMostOneElement(Parameter.findAllByType(passThrough))

        then:
        passThroughParameter

        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        ProcessingStep step2 = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition2))
        step2
        step == step2.previous
        step2 == step.next
        step2.input.isEmpty()

        when: "schedule the next job"
        schedulerService.schedule()
        List<Parameter> params = step2.output.toList().sort { it.value }

        then: "no Job should be be scheduled"
        areQueueAndRunningEmpty()
        step2.output

        and: "there should be three output parameters"
        3 == params.size()
        "1234" == params[0].value
        "1234" == params[1].value
        "4321" == params[2].value
        params.contains(passThroughParameter)
        process.finished
    }

    /**
     * Test that one output parameter can be mapped into multiple input parameters
     * and passthrough parameters of the next Job.
     */
    void "testOneToManyParameterMapping"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()
        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        JobDefinition jobDefinition2 = createTestEndStateAwareJob("test2", jep, jobDefinition)
        ParameterType passThrough = DomainFactory.createParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.PASSTHROUGH)
        ParameterType passThrough2 = DomainFactory.createParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.PASSTHROUGH)

        ParameterMapping mapping = new ParameterMapping(job: jobDefinition2,
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition, "test")), to: passThrough)
        jobDefinition2.addToParameterMappings(mapping)
        ParameterMapping mapping2 = new ParameterMapping(job: jobDefinition2,
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition, "test")), to: passThrough2)
        jobDefinition2.addToParameterMappings(mapping2)

        ParameterMapping mapping3 = new ParameterMapping(job: jobDefinition2,
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition, "test")),
                to: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition2, "input")))
        jobDefinition2.addToParameterMappings(mapping3)
        ParameterMapping mapping4 = new ParameterMapping(job: jobDefinition2,
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition, "test")),
                to: CollectionUtils.atMostOneElement(ParameterType.findAllByJobDefinitionAndName(jobDefinition2, "input2")))
        jobDefinition2.addToParameterMappings(mapping4)
        jobDefinition.next = jobDefinition2
        jobDefinition.save(flush: true)
        jobDefinition2.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        expect:
        areQueueAndRunningEmpty()

        when:
        schedulerService.queue.add(step)

        then:
        CollectionUtils.atMostOneElement(Parameter.findAllByType(passThrough)) == null

        when:
        schedulerService.schedule()

        Parameter passThroughParameter = CollectionUtils.atMostOneElement(Parameter.findAllByType(passThrough))
        Parameter passThroughParameter2 = CollectionUtils.atMostOneElement(Parameter.findAllByType(passThrough2))

        then:
        passThroughParameter
        passThroughParameter2
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when:
        ProcessingStep step2 = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition2))

        then:
        step2
        step == step2.previous
        step2 == step.next
        step2.input
        2 == step2.input.size()

        when: "schedule the next Job"
        schedulerService.schedule()

        then: "no Job should be be scheduled and there should be three output parameters"
        areQueueAndRunningEmpty()
        step2.output

        when:
        List<Parameter> params = step2.output.toList().sort { it.value }

        then:
        4 == params.size()
        "1234" == params[0].value
        "1234" == params[1].value
        "1234" == params[2].value
        "4321" == params[3].value
        params.contains(passThroughParameter)
        params.contains(passThroughParameter2)
        process.finished
    }

    void "testCreateProcess"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        startJob.save(flush: true)
        jep.startJob = startJob
        jep.save(flush: true)

        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)
        StartJob job = Holders.grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep

        expect:
        job
        0 == Process.count()
        0 == ProcessingStep.count()

        when:
        schedulerService.createProcess(job, [])

        then:
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()
        1 == Process.count()
        1 == ProcessingStep.count()

        when:
        Process process = CollectionUtils.atMostOneElement(Process.findAllByJobExecutionPlan(jep))

        then:
        process
        schedulerService.queue.first().jobDefinition == jobDefinition
        !process.finished

        when:
        schedulerService.schedule()

        then:
        process.finished
        areQueueAndRunningEmpty()
    }

    void "testCreateProcessWithDisabledScheduler"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        startJob.save(flush: true)
        jep.startJob = startJob

        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        StartJob job = Holders.grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep

        SpringSecurityUtils.doWithAuth(ADMIN) {
            schedulerService.suspendScheduler()
        }

        expect:
        job

        when: "scheduler is suspended"
        schedulerService.createProcess(job, [])

        then:
        RuntimeException e = thrown()
        e.message.contains("Scheduler is disabled")

        cleanup:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            schedulerService.resumeScheduler()
        }
    }

    void "testCreateProcessWithParameters"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        startJob.save(flush: true)
        jep.startJob = startJob

        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        ParameterType type1 = DomainFactory.createParameterType(name: "test", jobDefinition: startJob, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType type2 = DomainFactory.createParameterType(name: "test2", jobDefinition: startJob, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType passthrough = DomainFactory.createParameterType(name: "passthrough", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.PASSTHROUGH)
        ParameterType input = DomainFactory.createParameterType(name: "input3", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)

        expect:
        startJob == type1.jobDefinition
        startJob == type2.jobDefinition
        jobDefinition == passthrough.jobDefinition
        jobDefinition == input.jobDefinition

        ParameterMapping mapping1 = new ParameterMapping(from: type1, to: passthrough, jobDefinition: jobDefinition)
        ParameterMapping mapping2 = new ParameterMapping(from: type2, to: input, jobDefinition: jobDefinition)
        jobDefinition.addToParameterMappings(mapping1)
        jobDefinition.addToParameterMappings(mapping2)
        jobDefinition.save(flush: true)

        when:
        StartJob job = Holders.grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep

        then:
        job
        0 == Process.count()
        0 == ProcessingStep.count()

        when:
        schedulerService.createProcess(job, [new Parameter(value: "1234", type: type1), new Parameter(value: "abcd", type: type2)])

        then:
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()
        1 == Process.count()
        1 == ProcessingStep.count()

        when:
        Process process = CollectionUtils.atMostOneElement(Process.findAllByJobExecutionPlan(jep))

        then:
        process
        schedulerService.queue.first().jobDefinition == jobDefinition
        !process.finished

        when:
        ProcessingStep step = schedulerService.queue.first()

        then:
        1 == step.input.size()
        1 == step.output.size()
        passthrough == step.output.toList()[0].type
        input == step.input.toList()[0].type

        when: "running the Job should create more output params"
        schedulerService.schedule()

        then:
        areQueueAndRunningEmpty()
        3 == step.output.size()
    }

    void "testCreateProcessWithProcessParameter"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        startJob.save(flush: true)
        jep.startJob = startJob

        JobDefinition jobDefinition = createTestEndStateAwareJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        ParameterType type1 = DomainFactory.createParameterType(name: "test", jobDefinition: startJob, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType type2 = DomainFactory.createParameterType(name: "test2", jobDefinition: startJob, parameterUsage: ParameterUsage.OUTPUT)
        ParameterType passthrough = DomainFactory.createParameterType(name: "passthrough", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.PASSTHROUGH)
        ParameterType input = DomainFactory.createParameterType(name: "input3", jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT)

        expect:
        startJob == type1.jobDefinition
        startJob == type2.jobDefinition
        jobDefinition == passthrough.jobDefinition
        jobDefinition == input.jobDefinition
        // create the ParameterMapping
        ParameterMapping mapping1 = new ParameterMapping(from: type1, to: passthrough, jobDefinition: jobDefinition)
        ParameterMapping mapping2 = new ParameterMapping(from: type2, to: input, jobDefinition: jobDefinition)
        jobDefinition.addToParameterMappings(mapping1)
        jobDefinition.addToParameterMappings(mapping2)
        jobDefinition.save(flush: true)

        when:
        StartJob job = Holders.grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep

        then:
        job
        0 == Process.count()
        0 == ProcessingStep.count()

        when:
        ProcessParameter processParameter = DomainFactory.createProcessParameter(className: SeqTrack.name, value: "test")
        schedulerService.createProcess(job, [], processParameter)

        then: "verify that the Process is created"
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()
        2 == Process.count()
        2 == ProcessingStep.count()

        when:
        Process process = CollectionUtils.atMostOneElement(Process.findAllByJobExecutionPlan(jep))

        then:
        process
        schedulerService.queue.first().jobDefinition == jobDefinition
        !process.finished

        when:
        schedulerService.schedule()

        then:
        areQueueAndRunningEmpty()
        "test" == CollectionUtils.atMostOneElement(ProcessParameter.findAllByProcess(process)).value
    }

    void "testDecisions"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        startJob.save(flush: true)
        jep.startJob = startJob

        DecidingJobDefinition decidingJobDefinition = new DecidingJobDefinition(name: "test", bean: "decisionTestJob", plan: jep)

        JobDecision decision1 = new JobDecision(jobDefinition: decidingJobDefinition, name: "outcome1", description: "test")
        decision1.save(flush: true)
        JobDecision decision2 = new JobDecision(jobDefinition: decidingJobDefinition, name: "outcome2", description: "test")
        decision2.save(flush: true)

        JobDefinition jobDefinition1 = createTestEndStateAwareJob("decision1", jep, decidingJobDefinition)
        jobDefinition1.save(flush: true)
        JobDefinition jobDefinition2 = new JobDefinition(name: "decision2", bean: "directTestJob", plan: jep, previous: decidingJobDefinition)
        jobDefinition2.save(flush: true)

        decidingJobDefinition.next = null
        decidingJobDefinition.save(flush: true)

        DecisionMapping mapping1 = new DecisionMapping(decision: decision1, definition: jobDefinition1)
        mapping1.save(flush: true)
        DecisionMapping mapping2 = new DecisionMapping(decision: decision2, definition: jobDefinition2)
        mapping2.save(flush: true)

        jep.firstJob = decidingJobDefinition
        jep.save(flush: true)

        when:
        StartJob job = Holders.grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep

        then:
        job
        0 == Process.count()
        0 == ProcessingStep.count()

        when:
        schedulerService.createProcess(job, [])

        then:
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()
        1 == Process.count()
        1 == ProcessingStep.count()
        1 == DecisionProcessingStep.count()

        when:
        ProcessingStep step = ProcessingStep.list().first()

        then:
        step instanceof DecisionProcessingStep
        (step as DecisionProcessingStep).decision == null

        when:
        Process process = CollectionUtils.atMostOneElement(Process.findAllByJobExecutionPlan(jep))

        then:
        process
        schedulerService.queue.first().jobDefinition == decidingJobDefinition

        when:
        schedulerService.schedule()

        then:
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()
        1 == schedulerService.queue.size()
        2 == ProcessingStep.count()
        1 == DecisionProcessingStep.count()

        decision1 == (step as DecisionProcessingStep).decision
        jobDefinition1 == schedulerService.queue.first().jobDefinition
        !Process.list().first().finished

        when:
        schedulerService.schedule()

        then:
        areQueueAndRunningEmpty()
        Process.list().first().finished
    }

    void "testFailingEndOfProcess"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        StartJobDefinition startJob = new StartJobDefinition(name: "start", bean: "testStartJob", plan: jep)
        startJob.save(flush: true)
        jep.startJob = startJob
        jep.save(flush: true)

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        StartJob job = Holders.grailsApplication.mainContext.getBean("testStartJob") as StartJob
        job.jobExecutionPlan = jep

        expect:
        job
        0 == Process.count()
        0 == ProcessingStep.count()

        when:
        schedulerService.createProcess(job, [])
        schedulerService.schedule()

        then:
        thrown(SchedulerException)
    }

    void "testFailingValidation"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)
        ValidatingJobDefinition validator = new ValidatingJobDefinition(name: "validator", bean: "failingValidatingTestJob", validatorFor: jobDefinition, plan: jep)
        validator.save(flush: true)

        jobDefinition.next = validator
        jobDefinition.save(flush: true)

        JobDefinition jobDefinition2 = createTestEndStateAwareJob("testEndStateAware", jep)
        jobDefinition2.save(flush: true)

        validator.next = jobDefinition2
        validator.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        when:
        schedulerService.queue.add(step)
        schedulerService.schedule()

        then:
        !process.finished
        3 == ProcessingStepUpdate.countByProcessingStep(step)

        when:
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }

        then:
        ExecutionState.CREATED == updates[0].state
        ExecutionState.STARTED == updates[1].state
        ExecutionState.FINISHED == updates[2].state
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when: "executing the validator should fail the process"
        schedulerService.schedule()

        then: "process should be finished and the step should be set to failure"
        process.finished
        4 == ProcessingStepUpdate.countByProcessingStep(step)

        when:
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        step = ProcessingStep.list().last()

        then:
        ExecutionState.CREATED == updates[0].state
        ExecutionState.STARTED == updates[1].state
        ExecutionState.FINISHED == updates[2].state
        ExecutionState.FAILURE == updates[3].state
        4 == ProcessingStepUpdate.countByProcessingStep(step)

        areQueueAndRunningEmpty()

        when:
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }

        then:
        ExecutionState.CREATED == updates[0].state
        ExecutionState.STARTED == updates[1].state
        ExecutionState.FINISHED == updates[2].state
        ExecutionState.SUCCESS == updates[3].state
    }

    void "testSuccessfulValidation"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        ValidatingJobDefinition validator = new ValidatingJobDefinition(name: "validator", bean: "validatingTestJob", validatorFor: jobDefinition, plan: jep)
        validator.save(flush: true)

        jobDefinition.next = validator
        jobDefinition.save(flush: true)

        JobDefinition jobDefinition2 = createTestEndStateAwareJob("testEndStateAware", jep)
        jobDefinition2.save(flush: true)

        validator.next = jobDefinition2
        validator.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        DomainFactory.createProcessingStepUpdate(processingStep: step)

        when:
        schedulerService.queue.add(step)
        schedulerService.schedule()
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }

        then:
        !process.finished
        3 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.CREATED == updates[0].state
        ExecutionState.STARTED == updates[1].state
        ExecutionState.FINISHED == updates[2].state

        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when:
        schedulerService.schedule()
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }

        then: "process should not yet be finished and the step should be set to success"
        !process.finished
        4 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.CREATED == updates[0].state
        ExecutionState.STARTED == updates[1].state
        ExecutionState.FINISHED == updates[2].state
        ExecutionState.SUCCESS == updates[3].state

        when:
        step = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(validator))
        updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }

        then:
        4 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.CREATED == updates[0].state
        ExecutionState.STARTED == updates[1].state
        ExecutionState.FINISHED == updates[2].state
        ExecutionState.SUCCESS == updates[3].state

        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when:
        schedulerService.schedule()

        then:
        process.finished
        areQueueAndRunningEmpty()
    }

    void "testRestartProcessingStepInCorrectState"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        jobDefinition.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep, finished: true)

        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }

        ProcessingStepUpdate update = DomainFactory.createProcessingStepUpdate(processingStep: step)
        // with a created event it should fail
        when:
        schedulerService.restartProcessingStep(step)

        then:
        thrown(IncorrectProcessingException)

        when: "with a started event it should fail"
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.STARTED,
                previous: update,
                processingStep: step,
        )
        schedulerService.restartProcessingStep(step)

        then:
        thrown(IncorrectProcessingException)

        when: "with a finished event it should fail"
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.FINISHED,
                previous: update,
                processingStep: step,
        )
        schedulerService.restartProcessingStep(step)

        then:
        thrown(IncorrectProcessingException)

        when: "with a success event it should fail"
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.SUCCESS,
                previous: update,
                processingStep: step,
        )
        schedulerService.restartProcessingStep(step)

        then:
        thrown(IncorrectProcessingException)

        when: "with a restarted event it should fail"
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.RESTARTED,
                previous: update,
                processingStep: step,
        )
        schedulerService.restartProcessingStep(step)

        then:
        thrown(IncorrectProcessingException)

        when: "with a suspended event it should fail"
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.SUSPENDED,
                previous: update,
                processingStep: step,
        )
        schedulerService.restartProcessingStep(step)

        then:
        thrown(IncorrectProcessingException)

        when: "with a resumed event it should fail"
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.RESUMED,
                previous: update,
                processingStep: step,
        )
        schedulerService.restartProcessingStep(step)

        then:
        thrown(IncorrectProcessingException)

        when: "set to failed"
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.FINISHED,
                previous: update,
                processingStep: step,
        )
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.FAILURE,
                previous: update,
                processingStep: step,
        )

        then:
        9 == ProcessingStepUpdate.countByProcessingStep(step)

        when:
        schedulerService.restartProcessingStep(step, false)

        then:
        10 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.RESTARTED == ProcessingStepUpdate.findAllByProcessingStep(step, [sort: 'id']).last().state
        !process.finished
        areQueueAndRunningEmpty()
    }

    void "testRestartProcessingStepProcessFinished"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        jobDefinition.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }

        mockProcessingStepAsFailed(step)

        expect:
        !process.finished

        when:
        schedulerService.restartProcessingStep(step, false)

        then:
        thrown(IncorrectProcessingException)

        4 == ProcessingStepUpdate.countByProcessingStep(step)

        when:
        process.finished = true
        process.save(flush: true)
        schedulerService.restartProcessingStep(step, false)

        then:
        5 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.RESTARTED == ProcessingStepUpdate.findAllByProcessingStep(step, [sort: 'id']).last().state
        !process.finished
        areQueueAndRunningEmpty()
    }

    void "testRestartProcessingStepHasUpdates"() {
        given:
        setupData()
        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        jobDefinition.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep, finished: true)

        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }

        when:
        schedulerService.restartProcessingStep(step, false)

        then:
        thrown(IncorrectProcessingException)

        when:
        mockProcessingStepAsFailed(step)
        schedulerService.restartProcessingStep(step, false)

        then:
        5 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.RESTARTED == ProcessingStepUpdate.findAllByProcessingStep(step, [sort: 'id']).last().state
        !process.finished
        areQueueAndRunningEmpty()
    }

    void "testRestartProcessingStep"() {
        given:
        setupData()
        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        jobDefinition.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep, finished: true)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }

        mockProcessingStepAsFailed(step)

        when:
        schedulerService.restartProcessingStep(step, false)

        then:
        5 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.RESTARTED == ProcessingStepUpdate.findAllByProcessingStep(step, [sort: 'id']).last().state
        !process.finished
        areQueueAndRunningEmpty()

        when:
        ProcessingStepUpdate update = ProcessingStepUpdate.findAllByProcessingStep(step, [sort: 'id']).last()
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.FINISHED,
                previous: update,
                processingStep: step
        )
        DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.FAILURE,
                previous: update,
                processingStep: step
        )

        then:
        7 == ProcessingStepUpdate.countByProcessingStep(step)

        when:
        process.finished = true
        process.save(flush: true)
        schedulerService.restartProcessingStep(step)

        then:
        8 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.RESTARTED == ProcessingStepUpdate.findAllByProcessingStep(step, [sort: 'id']).last().state
        !process.finished
        !schedulerService.queue.isEmpty()
        schedulerService.running.isEmpty()

        when:
        RestartedProcessingStep restartedStep = RestartedProcessingStep.findAllByOriginal(step, [sort: 'id']).last()

        then:
        restartedStep
        step == restartedStep.original
        1 == ProcessingStepUpdate.countByProcessingStep(restartedStep)
        ExecutionState.CREATED == ProcessingStepUpdate.findAllByProcessingStep(restartedStep, [sort: 'id']).last().state
        restartedStep == schedulerService.queue.first()

        when:
        schedulerService.schedule()

        then:
        8 == ProcessingStepUpdate.countByProcessingStep(step)
        3 == ProcessingStepUpdate.countByProcessingStep(restartedStep)
        ExecutionState.FINISHED == ProcessingStepUpdate.findAllByProcessingStep(restartedStep, [sort: 'id']).last().state
        !process.finished

        when:
        schedulerService.schedule()

        then:
        process.finished
        areQueueAndRunningEmpty()
        process.finished
    }

    /**
     * Tests that the previous next link is updated and the next job is run.
     */
    void "testRestartProcessingStepUpdatesLink"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        JobDefinition jobDefinition2 = createTestJob("test2", jep)
        jobDefinition.next = jobDefinition2
        jobDefinition.save(flush: true)

        JobDefinition jobDefinition3 = createTestEndStateAwareJob("test3", jep, jobDefinition2)
        jobDefinition2.next = jobDefinition3
        jobDefinition2.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)

        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return false }

        mockProcessingStepAsSucceeded(step)
        ProcessingStep step2 = DomainFactory.createProcessingStep(jobDefinition: jobDefinition2, process: process, previous: step)
        step2.metaClass.belongsToMultiJob = { -> return false }

        step.next = step2
        step.save(flush: true)

        expect:
        step.previous == null
        step2 == step.next
        step == step2.previous
        step2.next == null

        when:
        mockProcessingStepAsFailed(step2)

        process.finished = true
        process.save(flush: true)

        then: "restart step2"
        0 == RestartedProcessingStep.count()

        when:
        schedulerService.restartProcessingStep(step2)

        then:
        1 == RestartedProcessingStep.count()

        when:
        schedulerService.schedule()
        schedulerService.schedule()

        then:
        areQueueAndRunningEmpty()
        process.finished

        when:
        step = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition))
        RestartedProcessingStep restartedStep = CollectionUtils.atMostOneElement(RestartedProcessingStep.findAllByJobDefinition(jobDefinition2))
        ProcessingStep lastStep = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition3))

        then:
        step.previous == null

        restartedStep.previous == step
        step.next == restartedStep
        step2 == restartedStep.original
        step2.next == null

        restartedStep.next == lastStep
        lastStep.previous == restartedStep
        lastStep.next == null
    }

    /**
     * Test that the branching is done correctly.
     */
    void "testRestartProcessingStepKeepsLinks"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        jep.save(flush: true)

        JobDefinition jobDefinition2 = createTestJob("test2", jep)
        jobDefinition.next = jobDefinition2
        jobDefinition.save(flush: true)

        JobDefinition jobDefinition3 = createTestJob("test3", jep)
        jobDefinition2.next = jobDefinition3
        jobDefinition2.save(flush: true)

        JobDefinition jobDefinition4 = createTestEndStateAwareJob("test4", jep, jobDefinition3)
        jobDefinition3.next = jobDefinition4
        jobDefinition3.save(flush: true)

        jep.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)
        ProcessingStep firstStep = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)

        DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.CREATED,
                previous: null,
                processingStep: firstStep
        )

        when:
        schedulerService.queue << firstStep
        schedulerService.schedule()
        schedulerService.schedule()
        schedulerService.schedule()
        schedulerService.schedule()

        then:
        areQueueAndRunningEmpty()
        process.finished

        when: "get ProcessingSteps"
        ProcessingStep secondStep = CollectionUtils.atMostOneElement(ProcessingStep.findAllByJobDefinition(jobDefinition2))
        ProcessingStep thirdStep = secondStep.next
        ProcessingStep fourthStep = thirdStep.next

        then:
        secondStep
        thirdStep
        fourthStep

        when:
        DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.FAILURE,
                previous: ProcessingStepUpdate.findAllByProcessingStep(secondStep, [sort: 'id']).last(),
                processingStep: secondStep
        )

        schedulerService.restartProcessingStep(secondStep)
        schedulerService.schedule()
        schedulerService.schedule()
        schedulerService.schedule()

        then:
        areQueueAndRunningEmpty()
        process.finished

        and: "verify the chain of ProcessingSteps"
        1 == RestartedProcessingStep.count()
        1 == ProcessingStep.countByJobDefinition(jobDefinition)
        2 == ProcessingStep.countByJobDefinition(jobDefinition2)
        2 == ProcessingStep.countByJobDefinition(jobDefinition3)
        2 == ProcessingStep.countByJobDefinition(jobDefinition4)

        when:
        RestartedProcessingStep restartedStep = RestartedProcessingStep.list([sort: 'id']).last()

        then:
        firstStep.next == restartedStep
        restartedStep.previous == firstStep
        restartedStep.original == secondStep
        restartedStep.next != thirdStep

        secondStep.previous == firstStep
        secondStep.next == thirdStep
        thirdStep.previous == secondStep
        thirdStep.next == fourthStep
        fourthStep.previous == thirdStep

        fourthStep.next == null

        when:
        ProcessingStep thirdStep2 = ProcessingStep.findAllByJobDefinition(jobDefinition3, [sort: 'id']).last()

        then:
        thirdStep2 != thirdStep
        restartedStep.next == thirdStep2
        thirdStep2.previous == restartedStep

        when:
        ProcessingStep fourthStep2 = ProcessingStep.findAllByJobDefinition(jobDefinition4, [sort: 'id']).last()

        then:
        fourthStep2 != fourthStep
        thirdStep2.next == fourthStep2
        fourthStep2.previous == thirdStep2
        fourthStep2.next == null
    }

    /**
     * Test for BUG: #OTP-57
     */
    void "testRestartProcessingStepKeepsParameters"() {
        given:
        setupData()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition1 = createTestJob("test", jep)
        jep.firstJob = jobDefinition1
        jep.save(flush: true)

        JobDefinition jobDefinition2 = createTestJob("test2", jep)
        jobDefinition1.next = jobDefinition2
        jobDefinition1.save(flush: true)

        JobDefinition jobDefinition3 = createTestEndStateAwareJob("test4", jep, jobDefinition2)
        jobDefinition2.next = jobDefinition3
        jobDefinition2.save(flush: true)

        ParameterType type4 = DomainFactory.createParameterType(name: "passthrough", jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.PASSTHROUGH)
        ParameterType type5 = DomainFactory.createParameterType(name: "constant", jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.INPUT)

        ParameterMapping mapping1 = new ParameterMapping(
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByNameAndJobDefinition("test", jobDefinition1)),
                to: CollectionUtils.atMostOneElement(ParameterType.findAllByNameAndJobDefinition("input", jobDefinition2)), job: jobDefinition2)
        jobDefinition2.addToParameterMappings(mapping1)
        ParameterMapping mapping2 = new ParameterMapping(
                from: CollectionUtils.atMostOneElement(ParameterType.findAllByNameAndJobDefinition("test2", jobDefinition1)), to: type4, job: jobDefinition2)
        jobDefinition2.addToParameterMappings(mapping2)

        jobDefinition2.addToConstantParameters(DomainFactory.createParameter(type: type5, value: "foobar"))
        jobDefinition2.save(flush: true)

        Process process = DomainFactory.createProcess(jobExecutionPlan: jep)

        ProcessingStep firstStep = DomainFactory.createProcessingStep(jobDefinition: jobDefinition1, process: process)
        firstStep.metaClass.belongsToMultiJob = { -> return false }

        mockProcessingStepAsFinished(firstStep)
        firstStep.addToOutput(DomainFactory.createParameter(type: CollectionUtils.atMostOneElement(ParameterType.findAllByNameAndJobDefinition("test", jobDefinition1)), value: "bar"))
        firstStep.addToOutput(DomainFactory.createParameter(type: CollectionUtils.atMostOneElement(ParameterType.findAllByNameAndJobDefinition("test2", jobDefinition1)), value: "foo"))
        firstStep.save(flush: true)

        ProcessingStep secondStep = DomainFactory.createProcessingStep(jobDefinition: jobDefinition2, process: process, previous: firstStep)
        secondStep.metaClass.belongsToMultiJob = { -> return false }
        secondStep.save(flush: true)

        firstStep.next = secondStep
        firstStep.save(flush: true)

        mockProcessingStepAsFailed(secondStep)
        process.finished = true
        process.save(flush: true)

        when: "restart the failed Processing Step"
        schedulerService.restartProcessingStep(secondStep, false)
        RestartedProcessingStep restartedStep = RestartedProcessingStep.list([sort: 'id']).last()

        then:
        restartedStep
        secondStep == restartedStep.original
        !restartedStep.input.empty

        when:
        List<String> values = restartedStep.input*.value.sort()

        then:
        2 == values.size()
        "bar" == values[0]
        "foobar" == values[1]
        !restartedStep.output.empty
        1 == restartedStep.output.size()
        "foo" == restartedStep.output[0].value
    }

    private ProcessingStep createFailedProcessingStep() {
        areQueueAndRunningEmpty()

        JobExecutionPlan jep = DomainFactory.createJobExecutionPlan()

        JobDefinition jobDefinition = createTestJob("test", jep)
        jep.firstJob = jobDefinition
        assertNotNull(jep.save(flush: true))
        jobDefinition.next = createTestEndStateAwareJob("test2", jep, jobDefinition)
        assertNotNull(jobDefinition.save(flush: true))
        assertNotNull(jep.save(flush: true))
        // create the Process
        Process process = DomainFactory.createProcess(jobExecutionPlan: jep, finished: true)
        ProcessingStep step = DomainFactory.createProcessingStep(jobDefinition: jobDefinition, process: process)
        step.metaClass.belongsToMultiJob = { -> return true }
        mockProcessingStepAsFailed(step)
        return step
    }

    void "testRestartProcessingStep_WhenMultiJobResumeTrue"() {
        given:
        setupData()
        ProcessingStep step = createFailedProcessingStep()

        when:
        schedulerService.restartProcessingStep(step, false, true)

        then: "no RestartedProcessingSteps should be created"
        5 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.SUSPENDED == ProcessingStepUpdate.findAllByProcessingStep(step, [sort: 'id']).last().state
        [] == RestartedProcessingStep.findAllByOriginal(step)
        !step.process.finished
        areQueueAndRunningEmpty()
    }

    void "testRestartProcessingStep_WhenMultiJobResumeFalse"() {
        given:
        setupData()
        ProcessingStep step = createFailedProcessingStep()

        when:
        schedulerService.restartProcessingStep(step)

        then: "RestartedProcessingStep should be created"
        5 == ProcessingStepUpdate.countByProcessingStep(step)
        ExecutionState.RESTARTED == ProcessingStepUpdate.findAllByProcessingStep(step, [sort: 'id']).last().state
        1 == RestartedProcessingStep.count()
        !step.process.finished
    }

    @Unroll
    void "isJobResumable expect #result for #jobClass"() {
        given:
        setupData()
        ProcessingStep processingStep = DomainFactory.createProcessingStep(jobClass: jobClass)

        expect:
        schedulerService.isJobResumable(processingStep) == result

        where:
        jobClass                                      || result
        TestJob.class.name                            || false
        ResumableTestJob.class.name                   || true
        ResumableSometimesResumableTestJob.class.name || true
    }

    @Unroll
    void "isJobResumable for sometimes resumable job, with resumable = #resumable"() {
        given:
        setupData()

        when:
        final ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(SometimesResumableTestJob.class.name)
        final SometimesResumableTestJob job = new SometimesResumableTestJob()
        job.processingStep = processingStep
        job.resumable = resumable
        schedulerService.running << job

        then:
        schedulerService.isJobResumable(processingStep) == resumable

        where:
        resumable << [false, true]
    }

    void "testIsJobResumable_sometimesResumable_noRunningJob"() {
        given:
        setupData()
        final ProcessingStep processingStep = DomainFactory.createProcessingStep(jobClass: SometimesResumableTestJob.class.name)

        when:
        schedulerService.isJobResumable(processingStep)

        then:
        thrown(RuntimeException)
    }

    private boolean areQueueAndRunningEmpty() {
        return schedulerService.queue.isEmpty() && schedulerService.running.isEmpty()
    }

    private ProcessingStepUpdate mockProcessingStepAsFinished(ProcessingStep step) {
        ProcessingStepUpdate update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.CREATED,
                previous: null,
                processingStep: step
        )
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.STARTED,
                previous: update,
                processingStep: step
        )
        update = DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.FINISHED,
                previous: update,
                processingStep: step
        )
        return update
    }

    private void mockProcessingStepAsFailed(ProcessingStep step) {
        ProcessingStepUpdate update = mockProcessingStepAsFinished(step)
        DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.FAILURE,
                previous: update,
                processingStep: step
        )
    }

    private void mockProcessingStepAsSucceeded(ProcessingStep step) {
        ProcessingStepUpdate update = mockProcessingStepAsFinished(step)
        DomainFactory.createProcessingStepUpdate(
                state: ExecutionState.SUCCESS,
                previous: update,
                processingStep: step
        )
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

    /**
     * Creates a JobDefinition for the testEndStateAwareJob.
     * @param name Name of the JobDefinition
     * @param jep The JobExecutionPlan this JobDefinition will belong to
     * @param previous The previous Job Execution plan (optional)
     * @return Created JobDefinition
     * @deprecated this was copied here to be able to delete AbstractIntegrationTest. Don't use it, refactor it.
     */
    @Deprecated
    protected JobDefinition createTestEndStateAwareJob(String name, JobExecutionPlan jep, JobDefinition previous = null, String beanName = "testEndStateAwareJob") {
        JobDefinition jobDefinition = new JobDefinition(name: name, bean: beanName, plan: jep, previous: previous)
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
