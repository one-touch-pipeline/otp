/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflow.restartHandler

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

// for the spock matching, using closure as parameter is better readable
class AutoRestartHandlerServiceSpec extends Specification implements ServiceUnitTest<AutoRestartHandlerService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowJobErrorDefinition,
        ]
    }

    private List<JobErrorDefinitionWithLogWithIdentifier> createDefinition(Map map1 = [:], Map map2 = [:]) {
        return [
                new JobErrorDefinitionWithLogWithIdentifier([
                        errorDefinition: new WorkflowJobErrorDefinition([
                                action              : WorkflowJobErrorDefinition.Action.RESTART_JOB,
                                beanToRestart       : 'bean1',
                                allowRestartingCount: 1,

                        ] + map1),
                ]),
                new JobErrorDefinitionWithLogWithIdentifier([
                        errorDefinition: new WorkflowJobErrorDefinition([
                                action              : WorkflowJobErrorDefinition.Action.RESTART_JOB,
                                beanToRestart       : 'bean1',
                                allowRestartingCount: 1,
                        ] + map2),
                ]),
        ]
    }

    private void initService() {
        service.workflowJobErrorDefinitionService = Mock(WorkflowJobErrorDefinitionService)
        service.logService = Mock(LogService) {
            0 * _
        }
        service.errorNotificationService = Mock(ErrorNotificationService) {
            0 * _
        }
        service.autoRestartActionService = Mock(AutoRestartActionService) {
            0 * _
        }
    }

    void "handleRestarts, when no JobErrorDefinition found, then log and send mail with stop action"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        initService()

        when:
        service.handleRestarts(workflowStep)

        then:
        1 * service.workflowJobErrorDefinitionService.findMatchingJobErrorDefinition(workflowStep) >> []
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('AutoRestartHandler starting') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found no matching ErrorDefinition over all logs') }
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, [])
    }

    void "handleRestarts, when JobErrorDefinition found with different action, then log and send mail with stop action"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        List<JobErrorDefinitionWithLogWithIdentifier> definitions = createDefinition([
                action: WorkflowJobErrorDefinition.Action.RESTART_WORKFLOW,
        ])
        initService()

        when:
        service.handleRestarts(workflowStep)

        then:
        1 * service.workflowJobErrorDefinitionService.findMatchingJobErrorDefinition(workflowStep) >> definitions
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('AutoRestartHandler starting') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found 2 matching ErrorDefinition over all logs') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found following actions:') }
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, definitions)
    }

    void "handleRestarts, when JobErrorDefinition found with different beans to restart for action RESTART_JOB, then log and send mail with stop action"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        List<JobErrorDefinitionWithLogWithIdentifier> definitions = createDefinition([
                beanToRestart: 'bean2',
        ])
        initService()

        when:
        service.handleRestarts(workflowStep)

        then:
        1 * service.workflowJobErrorDefinitionService.findMatchingJobErrorDefinition(workflowStep) >> definitions
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('AutoRestartHandler starting') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found 2 matching ErrorDefinition over all logs') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single action:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found following beans for job restart:') }
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, definitions)
    }

    void "handleRestarts, when JobErrorDefinition found same bean to restart for action RESTART_JOB but job restart count is to high, then log and send mail with stop action"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                restartedFrom: createWorkflowStep()
        ])

        List<JobErrorDefinitionWithLogWithIdentifier> definitions = createDefinition()
        initService()

        when:
        service.handleRestarts(workflowStep)

        then:
        1 * service.workflowJobErrorDefinitionService.findMatchingJobErrorDefinition(workflowStep) >> definitions
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('AutoRestartHandler starting') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found 2 matching ErrorDefinition over all logs') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single action:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single bean for job restart:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('1 times restarted, ErrorDefinition allows 1') }
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, definitions)
    }

    void "handleRestarts, when JobErrorDefinition found same bean to restart for action RESTART_JOB but workflow restart count is to high, then log and send mail with stop action"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        restartedFrom: createWorkflowRun()
                ]),
        ])

        List<JobErrorDefinitionWithLogWithIdentifier> definitions = createDefinition()
        initService()

        when:
        service.handleRestarts(workflowStep)

        then:
        1 * service.workflowJobErrorDefinitionService.findMatchingJobErrorDefinition(workflowStep) >> definitions
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('AutoRestartHandler starting') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found 2 matching ErrorDefinition over all logs') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single action:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single bean for job restart:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('1 times restarted, ErrorDefinition allows 1') }
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, definitions)
    }

    void "handleRestarts, when JobErrorDefinition found same bean to restart for action RESTART_JOB but workflow and job restart count together is to high, then log and send mail with stop action"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                restartedFrom: createWorkflowStep([
                        workflowRun: createWorkflowRun([
                                restartedFrom: createWorkflowRun()
                        ]),
                ]),
        ])
        List<JobErrorDefinitionWithLogWithIdentifier> definitions = createDefinition([
                allowRestartingCount: 2
        ])
        initService()

        when:
        service.handleRestarts(workflowStep)

        then:
        1 * service.workflowJobErrorDefinitionService.findMatchingJobErrorDefinition(workflowStep) >> definitions
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('AutoRestartHandler starting') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found 2 matching ErrorDefinition over all logs') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single action:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single bean for job restart:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('2 times restarted, ErrorDefinition allows 2') }
        1 * service.errorNotificationService.send(workflowStep, WorkflowJobErrorDefinition.Action.STOP, _, definitions)
    }

    void "handleRestarts, when JobErrorDefinition found same bean to restart for action RESTART_JOB and restarts are fine, then log and call handleAction "() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        List<JobErrorDefinitionWithLogWithIdentifier> definitions = createDefinition()
        initService()

        when:
        service.handleRestarts(workflowStep)

        then:
        1 * service.workflowJobErrorDefinitionService.findMatchingJobErrorDefinition(workflowStep) >> definitions
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('AutoRestartHandler starting') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found 2 matching ErrorDefinition over all logs') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single action:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('found single bean for job restart:') }
        1 * service.logService.addSimpleLogEntry(workflowStep) { String message -> message.contains('0 times restarted, ErrorDefinition allows 1') }
        1 * service.autoRestartActionService.handleActionAndSendMail(workflowStep, definitions, WorkflowJobErrorDefinition.Action.RESTART_JOB, 'bean1')
    }
}
