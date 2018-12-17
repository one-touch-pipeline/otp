package de.dkfz.tbi.otp.job.restarting

import grails.test.mixin.Mock
import org.apache.commons.logging.Log
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Mock([
        JobErrorDefinition,
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingError,
        ProcessingStep,
        ProcessingStepUpdate,
])
class RestartHandlerServiceSpec extends Specification {

    @Unroll
    void "test handleRestart"() {
        given:
        int countCall = isHandleTypeMessageAndCalledHandleActionCalled ? 1 : 0
        RestartHandlerService service = new RestartHandlerService(
                restartCheckerService: Mock(RestartCheckerService) {
                    isJobAlreadyRestarted(_) >> jobRestarted
                    isWorkflowAlreadyRestarted(_) >> workflowRestarted
                    canJobBeRestarted(_) >> canJobRestart
                    canWorkflowBeRestarted(_) >> canWorkflowRestart
                },
                restartParseService: Mock(RestartParseService) {
                    countCall * handleTypeMessage(_, _) >> JobErrorDefinition.Action.STOP
                    0 * _
                },
                restartActionService: Mock(RestartActionService) {
                    countCall * handleAction(_, _)
                    0 * _
                }
        )
        ProcessingError error = DomainFactory.createProcessingError()
        Job job = GroovyMock(Job) {
            _ * getLog() >> Mock(Log) {
                _ * debug(_)
            }
            _ * getProcessingStep() >> error.processingStep
        }
        if (hasJobErrorDefinitions) {
            DomainFactory.createJobErrorDefinition([jobDefinitions: [error.processingStep.jobDefinition]])
        }

        when:
        service.handleRestart(job)

        then:
        noExceptionThrown()

        where:
        jobRestarted | workflowRestarted | canJobRestart | canWorkflowRestart | hasJobErrorDefinitions || isHandleTypeMessageAndCalledHandleActionCalled
        false        | false             | true          | true               | true                   || true
        true         | false             | true          | true               | true                   || false
        false        | true              | true          | true               | true                   || false
        true         | true              | true          | true               | true                   || false
        false        | false             | false         | true               | true                   || true
        false        | false             | true          | false              | true                   || true
        false        | false             | false         | false              | true                   || false
        false        | false             | true          | true               | false                  || false
    }
}
