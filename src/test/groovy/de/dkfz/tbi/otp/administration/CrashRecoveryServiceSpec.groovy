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
package de.dkfz.tbi.otp.administration

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import spock.lang.Specification

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class CrashRecoveryServiceSpec extends Specification implements ServiceUnitTest<CrashRecoveryService>, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Parameter,
                ProcessingError,
                ProcessingStepUpdate,
        ]
    }

    static final int COUNT_OF_ELEMENTS = 3

    static final String ERROR_MESSAGE = 'Error message'

    /**
     * Counter of method service.schedulerService.createNextProcessingStep
     */
    int callCreateNextProcessingStepCount = 0

    /**
     * Counter of method service.schedulerService.restartProcessingStep
     */
    int callRestartProcessingStepCount = 0

    List<ParameterType> parameterTypeList = []

    List<ProcessingStep> processingSteps = []

    Map<Long, Map<Long, String>> parameters = [:]

    void createServices(boolean startUpOk = false, boolean allowCallOfCreateNextProcessingStep = false, boolean allowCallOfRestartProcessingStep = false) {
        service.schedulerService = [
                isStartupOk             : { -> startUpOk },
                createNextProcessingStep: { ProcessingStep previous ->
                    assert allowCallOfCreateNextProcessingStep
                    callCreateNextProcessingStepCount++
                },
                restartProcessingStep   : { ProcessingStep previous, boolean schedule, boolean resume3in1job ->
                    assert allowCallOfRestartProcessingStep
                    callRestartProcessingStepCount++
                },
        ] as SchedulerService
        service.processService = new ProcessService()
    }

    void createProcessingSteps(boolean createParameterTypes = false) {
        JobDefinition jobDefinition = DomainFactory.createJobDefinition(name: 'name')

        processingSteps = (1..COUNT_OF_ELEMENTS).collect {
            ProcessingStep processingStep = DomainFactory.createProcessingStep([
                    jobDefinition: jobDefinition,
                    process      : DomainFactory.createProcess([
                            jobExecutionPlan: jobDefinition.plan,
                    ])
            ])
            DomainFactory.createProcessingStepUpdate([
                    processingStep: processingStep,
                    state         : ExecutionState.CREATED,
            ])
            return processingStep
        }

        if (createParameterTypes) {
            parameterTypeList = (1..COUNT_OF_ELEMENTS).collect {
                DomainFactory.createParameterType([
                        parameterUsage: ParameterUsage.OUTPUT,
                        jobDefinition : jobDefinition,
                ])
            }

            parameters = processingSteps.collectEntries { processingStep ->
                [
                        processingStep.id,
                        parameterTypeList.collectEntries { parameterType ->
                            [
                                    parameterType.id,
                                    "value: ${processingStep.id} ${parameterType.name}",
                            ]
                        },
                ]
            }
        }
    }

    void "test isCrashRecovery, should return true"() {
        given:
        createServices(false)

        expect:
        service.isCrashRecovery()
    }

    void "test isCrashRecovery, should return false"() {
        given:
        createServices(true)

        expect:
        !service.isCrashRecovery()
    }

    void "test markJobsAsFinished, processing steps given by ID should be marked as finished and not have parameters"() {
        given:
        createServices(false, true)
        createProcessingSteps()
        parameters = [:]

        when:
        service.markJobsAsFinished(processingSteps*.id, parameters)
        processingSteps.each {
            assertExecutionState(it, [
                    ExecutionState.CREATED,
                    ExecutionState.FINISHED,
            ])
        }

        then:
        COUNT_OF_ELEMENTS == callCreateNextProcessingStepCount
    }

    void "test markJobsAsFinished, processing steps given by ID should be marked as finished and have parameters"() {
        given:
        createServices(false, true)
        createProcessingSteps(true)

        when:
        service.markJobsAsFinished(processingSteps*.id, parameters)
        processingSteps.each { processingStep ->
            assertExecutionState(processingStep, [
                    ExecutionState.CREATED,
                    ExecutionState.FINISHED,
            ])
            parameterTypeList.each { ParameterType parameterType ->
                Parameter parameter = processingStep.output.find { Parameter parameter ->
                    parameter.type == parameterType
                }
                assert parameter
                assert parameter.value == "value: ${processingStep.id} ${parameterType.name}"
            }
        }

        then:
        COUNT_OF_ELEMENTS == callCreateNextProcessingStepCount
    }

    void "test markJobsAsFinished, should fail if no IDs are given"() {
        given:
        createServices()

        when:
        service.markJobsAsFinished(null, parameters)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("ids")
    }

    void "test markJobsAsFinished, should fail if no parameters are given"() {
        given:
        createServices()
        List ids = [DomainFactory.createProcessingStep().id]
        Map parameters = null

        when:
        service.markJobsAsFinished(ids, parameters)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("parameters")
    }

    void "test markJobsAsFinished, should fail if IDs are unknown"() {
        given:
        createServices()
        List ids = [1]
        Map parameters = [:]

        when:
        service.markJobsAsFinished(ids, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("No ProcessingStep")
    }

    void "test markJobsAsFinished, should fail if crash recovery is not active"() {
        given:
        createServices(true)
        List ids = [DomainFactory.createProcessingStep().id]
        Map parameters = [:]

        when:
        service.markJobsAsFinished(ids, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "The system is not in Crash Recovery"
    }

    void "test markJobsAsFinished, should fail if processing step is already restarted"() {
        given:
        createServices()
        ProcessingStep step1 = DomainFactory.createProcessingStep()
        ProcessingStep step2 = DomainFactory.createProcessingStep([
                process      : step1.process,
                jobDefinition: DomainFactory.createJobDefinition([
                        plan: step1.jobDefinition.plan,
                ])
        ])
        step1.next = step2
        step2.previous = step1
        assert step1.save(flush: true)
        List ids = [step1.id]

        when:
        service.markJobsAsFinished(ids, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "ProcessingStep ${ids[0]} has already been restarted"
    }

    void "test markJobsAsFinished, should fail if no parameter map for processing steps"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameters.remove(processingSteps[0].id)
        assert COUNT_OF_ELEMENTS - 1 == parameters.size()

        when:
        service.markJobsAsFinished(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "Parameter for type ${parameterTypeList[0].id} has not been set"
    }

    void "test markJobsAsFinished, should fail if parameters missing for one processing step"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameters.values().iterator().next().remove(parameterTypeList[0].id)
        assert COUNT_OF_ELEMENTS == parameters.size()

        when:
        service.markJobsAsFinished(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "Parameter for type ${parameterTypeList[0].id} has not been set"
    }

    void "test markJobsAsFinished, should fail if parameter has wrong job definition"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameterTypeList[0].jobDefinition = DomainFactory.createJobDefinition()

        when:
        service.markJobsAsFinished(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "ParameterType with id ${parameterTypeList[0].id} does not belong to ProcessingStep with id ${processingSteps[0].id}"
    }

    void "test markJobsAsFinished, should fail if parameter has wrong parameter usage"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameterTypeList[0].parameterUsage = ParameterUsage.INPUT

        when:
        service.markJobsAsFinished(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "ParameterType with id ${parameterTypeList[0].id} is not an output Parameter"
    }

    void "test markJobsAsFinished, should fail if parameter is null"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameters.values().iterator().next().put(parameterTypeList[0].id, null)

        when:
        service.markJobsAsFinished(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "Parameter with type id ${parameterTypeList[0].id} and value null cannot be stored"
    }

    void "test markJobsAsFinished, should fail if processing step update fails"() {
        given:
        createServices()
        ProcessingStep processingStep = DomainFactory.createProcessingStep()

        when:
        service.markJobsAsFinished([processingStep.id], parameters)

        then:
        def e = thrown(ProcessingException)
        e.message == "Could not create a ${ExecutionState.FINISHED} Update for ProcessingStep ${processingStep.id}"
    }

    void "test markJobsAsSucceeded, processing steps given by ID should be marked as finished and not have parameters"() {
        given:
        createServices(false, true)
        createProcessingSteps()

        when:
        service.markJobsAsSucceeded(processingSteps*.id, parameters)

        then:
        processingSteps.each {
            assertExecutionState(it, [
                    ExecutionState.CREATED,
                    ExecutionState.FINISHED,
                    ExecutionState.SUCCESS,
            ])
        }
        COUNT_OF_ELEMENTS == callCreateNextProcessingStepCount
    }

    void "test markJobsAsSucceeded, processing steps given by ID should be marked as finished and have parameters"() {
        given:
        createServices(false, true)
        createProcessingSteps(true)

        when:
        service.markJobsAsSucceeded(processingSteps*.id, parameters)

        then:
        processingSteps.each { processingStep ->
            assertExecutionState(processingStep, [
                    ExecutionState.CREATED,
                    ExecutionState.FINISHED,
                    ExecutionState.SUCCESS,
            ])
            parameterTypeList.each { ParameterType parameterType ->
                Parameter parameter = processingStep.output.find { Parameter parameter ->
                    parameter.type == parameterType
                }
                assert parameter
                assert parameter.value == "value: ${processingStep.id} ${parameterType.name}"
            }
        }
        COUNT_OF_ELEMENTS == callCreateNextProcessingStepCount
    }

    void "test markJobsAsSucceeded, should fail if no IDs are given"() {
        given:
        createServices()

        when:
        service.markJobsAsSucceeded(null, parameters)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("ids")
    }

    void "test markJobsAsSucceeded, should fail if no parameters are given"() {
        given:
        createServices()
        List ids = [DomainFactory.createProcessingStep().id]
        Map parameters = null

        when:
        service.markJobsAsSucceeded(ids, parameters)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("parameters")
    }

    void "test markJobsAsSucceeded, should fail if IDs are unknown"() {
        given:
        createServices()
        List ids = [1]
        Map parameters = [:]

        when:
        service.markJobsAsSucceeded(ids, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("No ProcessingStep")
    }

    void "test markJobsAsSucceeded, should fail if crash recovery is not active"() {
        given:
        createServices(true)
        List ids = [DomainFactory.createProcessingStep().id]
        Map parameters = [:]

        when:
        service.markJobsAsSucceeded(ids, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "The system is not in Crash Recovery"
    }

    void "test markJobsAsSucceeded, should fail if processing step is already restarted"() {
        given:
        createServices()
        ProcessingStep step1 = DomainFactory.createProcessingStep()
        ProcessingStep step2 = DomainFactory.createProcessingStep([
                process      : step1.process,
                jobDefinition: DomainFactory.createJobDefinition([
                        plan: step1.jobDefinition.plan,
                ])
        ])
        step1.next = step2
        step2.previous = step1
        assert step1.save(flush: true)
        List ids = [step1.id]

        when:
        service.markJobsAsSucceeded(ids, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "ProcessingStep ${ids[0]} has already been restarted"
    }

    void "test markJobsAsSucceeded, should fail if no parameter map for processing steps"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameters.remove(processingSteps[0].id)
        assert COUNT_OF_ELEMENTS - 1 == parameters.size()

        when:
        service.markJobsAsSucceeded(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "Parameter for type ${parameterTypeList[0].id} has not been set"
    }

    void "test markJobsAsSucceeded, should fail if parameters missing for one processing step"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameters.values().iterator().next().remove(parameterTypeList[0].id)
        assert COUNT_OF_ELEMENTS == parameters.size()

        when:
        service.markJobsAsSucceeded(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "Parameter for type ${parameterTypeList[0].id} has not been set"
    }

    void "test markJobsAsSucceeded, should fail if parameter has wrong job definition"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameterTypeList[0].jobDefinition = DomainFactory.createJobDefinition()

        when:
        service.markJobsAsSucceeded(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "ParameterType with id ${parameterTypeList[0].id} does not belong to ProcessingStep with id ${processingSteps[0].id}"
    }

    void "test markJobsAsSucceeded, should fail if parameter has wrong parameter usage"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameterTypeList[0].parameterUsage = ParameterUsage.INPUT

        when:
        service.markJobsAsSucceeded(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "ParameterType with id ${parameterTypeList[0].id} is not an output Parameter"
    }

    void "test markJobsAsSucceeded, should fail if parameter is null"() {
        given:
        createServices()
        createProcessingSteps(true)
        parameters.values().iterator().next().put(parameterTypeList[0].id, null)

        when:
        service.markJobsAsSucceeded(processingSteps*.id, parameters)

        then:
        def e = thrown(RuntimeException)
        e.message == "Parameter with type id ${parameterTypeList[0].id} and value null cannot be stored"
    }

    void "test markJobsAsSucceeded, should fail if processing step are already restarted"() {
        given:
        createServices()
        ProcessingStep processingStep = DomainFactory.createProcessingStep()

        when:
        service.markJobsAsSucceeded([processingStep.id], parameters)

        then:
        def e = thrown(ProcessingException)
        e.message == "Could not create a ${ExecutionState.FINISHED} Update for ProcessingStep ${processingStep.id}"
    }

    void "test markJobsAsFailed, processing steps given by ID should be marked as failed"() {
        given:
        createServices(false, false, true)
        createProcessingSteps()

        when:
        service.markJobsAsFailed(processingSteps*.id, ERROR_MESSAGE)

        then:
        processingSteps.each {
            assertExecutionState(it, [
                    ExecutionState.CREATED,
                    ExecutionState.FINISHED,
                    ExecutionState.FAILURE,
            ], ERROR_MESSAGE)
        }
    }

    void "test markJobsAsFailed, should fail if no IDs are given"() {
        given:
        createServices()

        when:
        service.markJobsAsFailed(null, ERROR_MESSAGE)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("ids")
    }

    void "test markJobsAsFailed, should fail if reason is null"() {
        given:
        createServices()
        List ids = [DomainFactory.createProcessingStep().id]

        when:
        service.markJobsAsFailed(ids, null)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("reason")
    }

    void "test markJobsAsFailed, should fail if IDs are unknown"() {
        given:
        createServices()
        List ids = [1]

        when:
        service.markJobsAsFailed(ids, ERROR_MESSAGE)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("No ProcessingStep")
    }

    void "test markJobsAsFailed, should fail if crash recovery is not active"() {
        given:
        createServices(true)
        List ids = [DomainFactory.createProcessingStep().id]

        when:
        service.markJobsAsFailed(ids, ERROR_MESSAGE)

        then:
        def e = thrown(RuntimeException)
        e.message == "The system is not in Crash Recovery"
    }

    void "test markJobsAsFailed, should fail if processing step is already restarted"() {
        given:
        createServices()
        ProcessingStep step1 = DomainFactory.createProcessingStep()
        ProcessingStep step2 = DomainFactory.createProcessingStep([
                process      : step1.process,
                jobDefinition: DomainFactory.createJobDefinition([
                        plan: step1.jobDefinition.plan,
                ])
        ])
        step1.next = step2
        step2.previous = step1
        assert step1.save(flush: true)
        List ids = [step1.id]

        when:
        service.markJobsAsFailed(ids, ERROR_MESSAGE)

        then:
        def e = thrown(RuntimeException)
        e.message == "ProcessingStep ${ids[0]} has already been restarted"
    }

    void "test markJobsAsFailed, should fail if processing step update failed"() {
        given:
        createServices()
        ProcessingStep processingStep = DomainFactory.createProcessingStep()

        when:
        service.markJobsAsFailed([processingStep.id], ERROR_MESSAGE)

        then:
        def e = thrown(ProcessingException)
        e.message == "Could not create a ${ExecutionState.FINISHED} Update for ProcessingStep ${processingStep.id}"
    }

    void "test restartJobs, processing steps given by ID should be marked as failed"() {
        given:
        createServices(false, false, true)
        createProcessingSteps()

        when:
        service.restartJobs(processingSteps*.id, ERROR_MESSAGE)

        then:
        processingSteps.each {
            assertExecutionState(it, [
                    ExecutionState.CREATED,
                    ExecutionState.FINISHED,
                    ExecutionState.FAILURE,
            ], ERROR_MESSAGE)
        }
        0 == callCreateNextProcessingStepCount
        COUNT_OF_ELEMENTS == callRestartProcessingStepCount
    }

    void "test restartJobs, should fail if no IDs are given"() {
        given:
        createServices()

        when:
        service.restartJobs(null, ERROR_MESSAGE)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("ids")
    }

    void "test restartJobs, should fail if reson is null "() {
        given:
        createServices()
        List ids = [DomainFactory.createProcessingStep().id]

        when:
        service.restartJobs(ids, null)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("reason")
    }

    void "test restartJobs, should fail if IDs are unknown"() {
        given:
        createServices()
        List ids = [1]

        when:
        service.restartJobs(ids, ERROR_MESSAGE)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("No ProcessingStep")
    }

    void "test restartJobs, should fail if crash recovery is not active"() {
        given:
        createServices(true)
        List ids = [DomainFactory.createProcessingStep().id]

        when:
        service.restartJobs(ids, ERROR_MESSAGE)

        then:
        def e = thrown(RuntimeException)
        e.message == "The system is not in Crash Recovery"
    }

    void "test restartJobs, should fail if processing step is already restarted"() {
        given:
        createServices()
        ProcessingStep step1 = DomainFactory.createProcessingStep()
        ProcessingStep step2 = DomainFactory.createProcessingStep([
                process      : step1.process,
                jobDefinition: DomainFactory.createJobDefinition([
                        plan: step1.jobDefinition.plan,
                ])
        ])
        step1.next = step2
        step2.previous = step1
        assert step1.save(flush: true)
        List ids = [step1.id]

        when:
        service.restartJobs(ids, ERROR_MESSAGE)

        then:
        def e = thrown(RuntimeException)
        e.message == "ProcessingStep ${ids[0]} has already been restarted"
    }

    void "test restartJobs, should fail if processing step update failed"() {
        given:
        createServices()
        ProcessingStep processingStep = DomainFactory.createProcessingStep()

        when:
        service.restartJobs([processingStep.id], ERROR_MESSAGE)

        then:
        def e = thrown(ProcessingException)
        e.message == "Could not create a ${ExecutionState.FINISHED} Update for ProcessingStep ${processingStep.id}"
    }

    void "test crashedJobs, should return processing steps"() {
        given:
        createProcessingSteps()
        service.schedulerService = [
                isStartupOk                   : { -> false },
                retrieveRunningProcessingSteps: { -> processingSteps },
        ] as SchedulerService

        expect:
        processingSteps == service.crashedJobs()
    }

    void "test crashedJobs, should fail if crash recovery is not active"() {
        given:
        createServices(true)
        DomainFactory.createProcessingStep()

        when:
        service.crashedJobs()

        then:
        def e = thrown(RuntimeException)
        e.message == "Not in Crash Recovery"
    }

    void "test getOutputParametersOfJobs, should return the output parameters of the given processing step IDs having no parameters"() {
        given:
        createServices()
        createProcessingSteps()
        List<Map<String, Object>> expected = processingSteps.collect {
            [id: it.id, jobName: 'name', parameter: []]
        }

        expect:
        expected == service.getOutputParametersOfJobs(processingSteps*.id)
    }

    void "test getOutputParametersOfJobs, should return the output parameters of the given processing step IDs having parameters"() {
        given:
        createServices()
        createProcessingSteps(true)
        List<Map<String, Object>> expected = processingSteps.collect {
            [id: it.id, jobName: 'name', parameter: parameterTypeList]
        }

        expect:
        expected == service.getOutputParametersOfJobs(processingSteps*.id)
    }

    void "test getOutputParametersOfJobs, should fail if no IDs are given"() {
        given:
        createServices()

        when:
        service.getOutputParametersOfJobs(null)

        then:
        def e = thrown(PowerAssertionError)
        e.message.contains("ids")
    }

    void "test getOutputParametersOfJobs, should fail if IDs are unknown"() {
        given:
        createServices()
        List ids = [1]

        when:
        service.getOutputParametersOfJobs(ids)

        then:
        def e = thrown(RuntimeException)
        e.message.contains("No ProcessingStep")
    }

    void "test getOutputParametersOfJobs, should fail if crash recovery is not active"() {
        given:
        createServices(true)
        List ids = [DomainFactory.createProcessingStep().id]

        when:
        service.getOutputParametersOfJobs(ids)

        then:
        def e = thrown(RuntimeException)
        e.message == "The system is not in Crash Recovery"
    }

    private assertExecutionState(ProcessingStep step, List<ExecutionState> states, String errorMessage = null) {
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort { it.id }
        Process process = Process.get(step.process.id)

        assert states == updates*.state
        if (errorMessage) {
            ProcessingStepUpdate last = updates[-1]
            assert last.error
            assert errorMessage == last.error.errorMessage
            assert process.finished
        } else {
            assert !process.finished
        }
    }
}
