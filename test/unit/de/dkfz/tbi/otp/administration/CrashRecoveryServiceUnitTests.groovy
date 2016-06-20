
package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.codehaus.groovy.runtime.powerassert.*
import org.junit.*

@TestFor(CrashRecoveryService)
@TestMixin(GrailsUnitTestMixin)
@Build([
    Parameter,
    ProcessingError,
    ProcessingStepUpdate,
])
class CrashRecoveryServiceUnitTests {

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



    @After
    void tearDown() {
        parameterTypeList = null
        processingSteps = null
        parameters = null
    }



    void createServices(boolean startUpOk = false, boolean allowCallOfCreateNextProcessingStep = false, boolean allowCallOfRestartProcessingStep = false) {
        service.schedulerService = [
            isStartupOk: { -> startUpOk },
            createNextProcessingStep: { ProcessingStep previous ->
                assert allowCallOfCreateNextProcessingStep
                callCreateNextProcessingStepCount++
            },
            restartProcessingStep: { ProcessingStep previous, boolean schedule, boolean resume3in1job ->
                assert allowCallOfRestartProcessingStep
                callRestartProcessingStepCount++
            },
        ] as SchedulerService
        service.processService = new ProcessService()
    }

    void createProcessingSteps(boolean createparameterTypes = false) {
        JobDefinition jobDefinition = JobDefinition.build()

        processingSteps = (1..COUNT_OF_ELEMENTS).collect {
            ProcessingStep processingStep = ProcessingStep.build([
                jobDefinition: jobDefinition,
                process: Process.build([
                    jobExecutionPlan: jobDefinition.plan,
                ])
            ])
            ProcessingStepUpdate.build([
                processingStep: processingStep,
                state: ExecutionState.CREATED,
            ])
            return processingStep
        }

        if (createparameterTypes) {
            parameterTypeList = (1..COUNT_OF_ELEMENTS).collect {
                ParameterType.build([
                    parameterUsage: ParameterUsage.OUTPUT,
                    jobDefinition: jobDefinition
                ])
            }

            parameters = processingSteps.collectEntries { processingStep ->
                [
                    processingStep.id,
                    parameterTypeList.collectEntries { parameterType ->
                        [
                            parameterType.id,
                            "value: ${processingStep.id} ${parameterType.name}"
                        ]
                    }
                ]
            }
        }
    }



    @Test
    void test_isCrashRecovery_shouldReturn_true() {
        createServices(false)

        assert service.isCrashRecovery()
    }

    @Test
    void test_isCrashRecovery_shouldReturn_false() {
        createServices(true)

        assert !service.isCrashRecovery()
    }



    @Test
    void test_markJobsAsFinished_processingStepsGivenByIdShouldBeMarkedAsFinishedAndHaveNoParameters() {
        createServices(false, true)
        createProcessingSteps()
        parameters = [:]

        service.markJobsAsFinished(processingSteps*.id, parameters)
        processingSteps.each {
            assertExecutionState(it, [
                ExecutionState.CREATED,
                ExecutionState.FINISHED,
            ])
        }
        assert COUNT_OF_ELEMENTS == callCreateNextProcessingStepCount
    }

    @Test
    void test_markJobsAsFinished_processingStepsGivenByIdShouldBeMarkedAsFinishedAndHaveParameters() {
        createServices(false, true)
        createProcessingSteps(true)

        service.markJobsAsFinished(processingSteps*.id, parameters)
        processingSteps.each { processingStep ->
            assertExecutionState(processingStep, [
                ExecutionState.CREATED,
                ExecutionState.FINISHED,
            ])
            parameterTypeList.each { ParameterType parameterType ->
                Parameter parameter = processingStep.output.find { Parameter parameter->
                    parameter.type == parameterType
                }
                assert parameter : 'Could not found any parameter for ${parameterType} in ${step}'
                assert parameter.value == "value: ${processingStep.id} ${parameterType.name}"
            }
        }
        assert COUNT_OF_ELEMENTS == callCreateNextProcessingStepCount
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseNoIdsAreGiven() {
        createServices()

        assert shouldFail(PowerAssertionError) {
            service.markJobsAsFinished(null, parameters)
        }.contains("ids")
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseNoParametersAreGiven() {
        createServices()
        List ids = [ProcessingStep.build().id]
        Map parameters = null

        assert shouldFail(PowerAssertionError) {
            service.markJobsAsFinished(ids, null)
        }.contains("parameters")
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseIdsAreUnknown() {
        createServices()
        List ids = [1]
        Map parameters = [:]

        assert shouldFail(RuntimeException) {
            service.markJobsAsFinished(ids, parameters)
        }.contains("No ProcessingStep")
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseCrashRecoveryNotActive() {
        createServices(true)
        List ids = [ProcessingStep.build().id]
        Map parameters = [:]

        assert shouldFail(RuntimeException) {
            service.markJobsAsFinished(ids, parameters)
        } == "The system is not in Crash Recovery"
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseProcessingStepAlreadyRestarted() {
        createServices()
        ProcessingStep step1 = ProcessingStep.build()
        ProcessingStep step2 = ProcessingStep.build([
            process: step1.process,
            jobDefinition: JobDefinition.build([
                plan: step1.jobDefinition.plan
            ])
        ])
        step1.next = step2
        step2.previous = step1
        assert step1.save()
        List ids = [
            step1.id
        ]

        assert shouldFail(RuntimeException) {
            service.markJobsAsFinished(ids, parameters)
        } == "ProcessingStep ${ids[0]} has already been restarted"
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseNoParameterMapForProcessingStep() {
        createServices()
        createProcessingSteps(true)
        parameters.remove(processingSteps[0].id)
        assert COUNT_OF_ELEMENTS - 1 == parameters.size()

        assert shouldFail(RuntimeException) {
            service.markJobsAsFinished(processingSteps*.id, parameters)
        } == "Parameter for type ${parameterTypeList[0].id} has not been set"
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseMissingParameterForOneProcessingStep() {
        createServices()
        createProcessingSteps(true)
        parameters.values().iterator().next().remove(parameterTypeList[0].id)
        assert COUNT_OF_ELEMENTS == parameters.size()

        assert shouldFail(RuntimeException) {
            service.markJobsAsFinished(processingSteps*.id, parameters)
        } == "Parameter for type ${parameterTypeList[0].id} has not been set"
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseParameterHasWrongJobDefinition() {
        createServices()
        createProcessingSteps(true)
        parameterTypeList[0].jobDefinition = JobDefinition.build()

        assert shouldFail(RuntimeException) {
            service.markJobsAsFinished(processingSteps*.id, parameters)
        } == "ParameterType with id ${parameterTypeList[0].id} does not belong to ProcessingStep with id ${processingSteps[0].id}"
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseParameterHasWrongParameterUsage() {
        createServices()
        createProcessingSteps(true)
        parameterTypeList[0].parameterUsage = ParameterUsage.INPUT

        assert shouldFail(RuntimeException) {
            service.markJobsAsFinished(processingSteps*.id, parameters)
        } == "ParameterType with id ${parameterTypeList[0].id} is not an output Parameter"
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseParameterHasNullAsValue() {
        createServices()
        createProcessingSteps(true)
        parameters.values().iterator().next().put(parameterTypeList[0].id, null)

        assert shouldFail(RuntimeException) {
            service.markJobsAsFinished(processingSteps*.id, parameters)
        } == "Parameter with type id ${parameterTypeList[0].id} and value null cannot be stored"
    }

    @Test
    void test_markJobsAsFinished_ShouldFailBecauseProcessingStepUpdateFail() {
        createServices()
        ProcessingStep processingStep = ProcessingStep.build()

        assert shouldFail(ProcessingException) {
            service.markJobsAsFinished([processingStep.id], parameters)
        } == "Could not create a ${ExecutionState.FINISHED} Update for ProcessingStep ${processingStep.id}"
    }



    @Test
    void test_markJobsAsSucceeded_ProcessingStepsGivenByIdShouldBeMarkedAsFinishedAndHaveNoParameters() {
        createServices(false, true)
        createProcessingSteps()

        service.markJobsAsSucceeded(processingSteps*.id, parameters)
        processingSteps.each {
            assertExecutionState(it, [
                ExecutionState.CREATED,
                ExecutionState.FINISHED,
                ExecutionState.SUCCESS,
            ])
        }
        assert COUNT_OF_ELEMENTS == callCreateNextProcessingStepCount
    }

    @Test
    void test_markJobsAsSucceeded_processingStepsGivenByIdShouldBeMarkedAsFinishedAndHaveParameters() {
        createServices(false, true)
        createProcessingSteps(true)

        service.markJobsAsSucceeded(processingSteps*.id, parameters)
        processingSteps.each { processingStep ->
            assertExecutionState(processingStep, [
                ExecutionState.CREATED,
                ExecutionState.FINISHED,
                ExecutionState.SUCCESS,
            ])
            parameterTypeList.each { ParameterType parameterType->
                Parameter parameter = processingStep.output.find { Parameter parameter->
                    parameter.type == parameterType
                }
                assert parameter : 'Could not found any parameter for ${parameterType} in ${step}'
                assert parameter.value == "value: ${processingStep.id} ${parameterType.name}"
            }
        }
        assert COUNT_OF_ELEMENTS == callCreateNextProcessingStepCount
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseNoIdsAreGiven() {
        createServices()

        assert shouldFail(PowerAssertionError) {
            service.markJobsAsSucceeded(null, parameters)
        }.contains("ids")
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseNoParametersAreGiven() {
        createServices()
        List ids = [ProcessingStep.build().id]
        Map parameters = null

        assert shouldFail(PowerAssertionError) {
            service.markJobsAsSucceeded(ids, null)
        }.contains("parameters")
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseIdsAreUnknown() {
        createServices()
        List ids = [1]
        Map parameters = [:]

        assert shouldFail(RuntimeException) {
            service.markJobsAsSucceeded(ids, parameters)
        }.contains("No ProcessingStep")
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseCrashRecoveryNotActive() {
        createServices(true)
        List ids = [ProcessingStep.build().id]
        Map parameters = [:]

        assert shouldFail(RuntimeException) {
            service.markJobsAsSucceeded(ids, parameters)
        } == "The system is not in Crash Recovery"
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseProcessingStepAlreadyRestarted() {
        createServices()
        ProcessingStep step1 = ProcessingStep.build()
        ProcessingStep step2 = ProcessingStep.build([
            process: step1.process,
            jobDefinition: JobDefinition.build([
                plan: step1.jobDefinition.plan
            ])
        ])
        step1.next = step2
        step2.previous = step1
        assert step1.save()
        List ids = [
            step1.id
        ]

        assert shouldFail(RuntimeException) {
            service.markJobsAsSucceeded(ids, parameters)
        } == "ProcessingStep ${ids[0]} has already been restarted"
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseNoParameterMapForProcessingStep() {
        createServices()
        createProcessingSteps(true)
        parameters.remove(processingSteps[0].id)
        assert COUNT_OF_ELEMENTS - 1 == parameters.size()

        assert shouldFail(RuntimeException) {
            service.markJobsAsSucceeded(processingSteps*.id, parameters)
        } == "Parameter for type ${parameterTypeList[0].id} has not been set"
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseMissingParameterForOneProcessingStep() {
        createServices()
        createProcessingSteps(true)
        parameters.values().iterator().next().remove(parameterTypeList[0].id)
        assert COUNT_OF_ELEMENTS == parameters.size()

        assert shouldFail(RuntimeException) {
            service.markJobsAsSucceeded(processingSteps*.id, parameters)
        } == "Parameter for type ${parameterTypeList[0].id} has not been set"
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseParameterHasWrongJobDefinition() {
        createServices()
        createProcessingSteps(true)
        parameterTypeList[0].jobDefinition = JobDefinition.build()

        assert shouldFail(RuntimeException) {
            service.markJobsAsSucceeded(processingSteps*.id, parameters)
        } == "ParameterType with id ${parameterTypeList[0].id} does not belong to ProcessingStep with id ${processingSteps[0].id}"
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseParameterHasWrongParameterUsage() {
        createServices()
        createProcessingSteps(true)
        parameterTypeList[0].parameterUsage = ParameterUsage.INPUT

        assert shouldFail(RuntimeException) {
            service.markJobsAsSucceeded(processingSteps*.id, parameters)
        } == "ParameterType with id ${parameterTypeList[0].id} is not an output Parameter"
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseParameterHasNullAsValue() {
        createServices()
        createProcessingSteps(true)
        parameters.values().iterator().next().put(parameterTypeList[0].id, null)

        assert shouldFail(RuntimeException) {
            service.markJobsAsSucceeded(processingSteps*.id, parameters)
        } == "Parameter with type id ${parameterTypeList[0].id} and value null cannot be stored"
    }

    @Test
    void test_markJobsAsSucceeded_ShouldFailBecauseProcessingStepUpdateFail() {
        createServices()
        ProcessingStep processingStep = ProcessingStep.build()

        assert shouldFail(ProcessingException) {
            service.markJobsAsSucceeded([processingStep.id], parameters)
        } == "Could not create a ${ExecutionState.FINISHED} Update for ProcessingStep ${processingStep.id}"
    }



    @Test
    void test_markJobsAsFailed_processingStepsGivenByIdShouldBeMarkedAsFailed() {
        createServices(false, false, true)
        createProcessingSteps()

        service.markJobsAsFailed(processingSteps*.id, ERROR_MESSAGE)
        processingSteps.each {
            assertExecutionState(it, [
                ExecutionState.CREATED,
                ExecutionState.FINISHED,
                ExecutionState.FAILURE,
            ], ERROR_MESSAGE)
        }
    }

    @Test
    void test_markJobsAsFailed_ShouldFailBecauseNoIdsAreGiven() {
        createServices()

        assert shouldFail(PowerAssertionError) {
            service.markJobsAsFailed(null, ERROR_MESSAGE)
        }.contains("ids")
    }

    @Test
    void test_markJobsAsFailed_ShouldFailBecauseNoReasonAreGiven() {
        createServices()
        List ids = [ProcessingStep.build().id]
        Map parameters = null

        assert shouldFail(PowerAssertionError) {
            service.markJobsAsFailed(ids, null)
        }.contains("reason")
    }

    @Test
    void test_markJobsAsFailed_ShouldFailBecauseIdsAreUnknown() {
        createServices()
        List ids = [1]

        assert shouldFail(RuntimeException) {
            service.markJobsAsFailed(ids, ERROR_MESSAGE)
        }.contains("No ProcessingStep")
    }

    @Test
    void test_markJobsAsFailed_ShouldFailBecauseCrashRecoveryNotActive() {
        createServices(true)
        List ids = [ProcessingStep.build().id]

        assert shouldFail(RuntimeException) {
            service.markJobsAsFailed(ids, ERROR_MESSAGE)
        } == "The system is not in Crash Recovery"
    }

    @Test
    void test_markJobsAsFailed_ShouldFailBecauseProcesingStepAlreadyRestarted() {
        createServices()
        ProcessingStep step1 = ProcessingStep.build()
        ProcessingStep step2 = ProcessingStep.build([
            process: step1.process,
            jobDefinition: JobDefinition.build([
                plan: step1.jobDefinition.plan
            ])
        ])
        step1.next = step2
        step2.previous = step1
        assert step1.save()
        List ids = [
            step1.id
        ]

        assert shouldFail(RuntimeException) {
            service.markJobsAsFailed(ids, ERROR_MESSAGE)
        } == "ProcessingStep ${ids[0]} has already been restarted"
    }

    @Test
    void test_markJobsAsFailed_ShouldFailBecauseProcessingStepUpdateFail() {
        createServices()
        ProcessingStep processingStep = ProcessingStep.build()

        assert shouldFail(ProcessingException) {
            service.markJobsAsFailed([processingStep.id], ERROR_MESSAGE)
        } == "Could not create a ${ExecutionState.FINISHED} Update for ProcessingStep ${processingStep.id}"
    }



    @Test
    void test_restartJobs_processingStepsGivenByIdShouldBeMarkedAsFailed() {
        createServices(false, false, true)
        createProcessingSteps()

        service.restartJobs(processingSteps*.id, ERROR_MESSAGE)
        processingSteps.each {
            assertExecutionState(it, [
                ExecutionState.CREATED,
                ExecutionState.FINISHED,
                ExecutionState.FAILURE,
            ], ERROR_MESSAGE)
        }
        assert 0 == callCreateNextProcessingStepCount
        assert COUNT_OF_ELEMENTS == callRestartProcessingStepCount
    }

    @Test
    void test_restartJobs_ShouldFailBecauseNoIdsAreGiven() {
        createServices()

        assert shouldFail(PowerAssertionError) {
            service.restartJobs(null, ERROR_MESSAGE)
        }.contains("ids")
    }

    @Test
    void test_restartJobs_ShouldFailBecauseNoReasonAreGiven() {
        createServices()
        List ids = [ProcessingStep.build().id]
        Map parameters = null

        assert shouldFail(PowerAssertionError) {
            service.restartJobs(ids, null)
        }.contains("reason")
    }

    @Test
    void test_restartJobs_ShouldFailBecauseIdsAreUnknown() {
        createServices()
        List ids = [1]
        Map parameters = [:]

        assert shouldFail(RuntimeException) {
            service.restartJobs(ids, ERROR_MESSAGE)
        }.contains("No ProcessingStep")
    }

    @Test
    void test_restartJobs_ShouldFailBecauseCrashRecoveryNotActive() {
        createServices(true)
        List ids = [ProcessingStep.build().id]
        Map parameters = [:]

        assert shouldFail(RuntimeException) {
            service.restartJobs(ids, ERROR_MESSAGE)
        } == "The system is not in Crash Recovery"
    }

    @Test
    void test_restartJobs_ShouldFailBecauseProcesingStepAlreadyRestarted() {
        createServices()
        ProcessingStep step1 = ProcessingStep.build()
        ProcessingStep step2 = ProcessingStep.build([
            process: step1.process,
            jobDefinition: JobDefinition.build([
                plan: step1.jobDefinition.plan
            ])
        ])
        step1.next = step2
        step2.previous = step1
        assert step1.save()
        List ids = [
            step1.id
        ]

        assert shouldFail(RuntimeException) {
            service.restartJobs(ids, ERROR_MESSAGE)
        } == "ProcessingStep ${ids[0]} has already been restarted"
    }

    @Test
    void test_restartJobs_ShouldFailBecauseProcessingStepUpdateFail() {
        createServices()
        ProcessingStep processingStep = ProcessingStep.build()

        assert shouldFail(ProcessingException) {
            service.restartJobs([processingStep.id], ERROR_MESSAGE)
        } == "Could not create a ${ExecutionState.FINISHED} Update for ProcessingStep ${processingStep.id}"
    }



    @Test
    void test_crashJobs_shouldReturnProcessingSteps() {
        createProcessingSteps()
        service.schedulerService = [
            isStartupOk: { -> false },
            retrieveRunningProcessingSteps: { -> processingSteps },
        ] as SchedulerService

        assert processingSteps == service.crashedJobs()
    }

    @Test
    void test_crashJobs_ShouldFailBecauseCrashRecoveryNotActive() {
        createServices(true)
        ProcessingStep processingStep = ProcessingStep.build()

        assert shouldFail(RuntimeException) { service.crashedJobs() } == "Not in Crash Recovery"
    }



    @Test
    void test_getOutputParametersOfJobs_shouldReturnTheOutputParametersOfTheGivenProcessingStepIdsHavingNoParameters() {
        createServices()
        createProcessingSteps()
        List<Map<String, Object>> expected = processingSteps.collect {
            [id: it.id, jobName: 'name', parameter: []]
        }

        List<Map<String, Object>> values = service.getOutputParametersOfJobs(processingSteps*.id)
        assert expected == values
    }

    @Test
    void test_getOutputParametersOfJobs_shouldReturnTheOutputParametersOfTheGivenProcessingStepIdsHavingParameters() {
        createServices()
        createProcessingSteps(true)
        List<Map<String, Object>> expected = processingSteps.collect {
            [id: it.id, jobName: 'name', parameter: parameterTypeList]
        }

        List<Map<String, Object>> values = service.getOutputParametersOfJobs(processingSteps*.id)
        assert expected == values
    }

    @Test
    void test_getOutputParametersOfJobs_ShouldFailBecauseNoIdsAreGiven() {
        createServices()

        assert shouldFail(PowerAssertionError) { service.getOutputParametersOfJobs(null) }.contains("ids")
    }

    @Test
    void test_getOutputParametersOfJobs_ShouldFailBecauseIdsAreUnknown() {
        createServices()
        List ids = [1]
        Map parameters = [:]

        assert shouldFail(RuntimeException) { service.getOutputParametersOfJobs(ids) }.contains("No ProcessingStep")
    }

    @Test
    void test_getOutputParametersOfJobs_ShouldFailBecauseCrashRecoveryNotActive() {
        createServices(true)
        List ids = [ProcessingStep.build().id]
        Map parameters = [:]

        assert shouldFail(RuntimeException) { service.getOutputParametersOfJobs(ids) } == "The system is not in Crash Recovery"
    }




    private assertExecutionState(ProcessingStep step, List<ExecutionState> states, String errorMessage = null) {
        List<ProcessingStepUpdate> updates = ProcessingStepUpdate.findAllByProcessingStep(step).sort{ it.id }
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
