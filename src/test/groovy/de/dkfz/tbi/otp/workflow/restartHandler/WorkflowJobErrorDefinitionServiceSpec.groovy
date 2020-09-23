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

class WorkflowJobErrorDefinitionServiceSpec extends Specification
        implements ServiceUnitTest<WorkflowJobErrorDefinitionService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowJobErrorDefinition,
        ]
    }

    void "findMatchingJobErrorDefinition, when no JobErrorDefinition available for bean, then return empty list and log stop 'no JobErrorDefinition'"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()

        service.logService = Mock(LogService) {
            1 * addSimpleLogEntry(workflowStep) { String message -> message.contains('no JobErrorDefinition found ') }
            0 * _
        }

        when:
        List<JobErrorDefinitionWithLogWithIdentifier> results = service.findMatchingJobErrorDefinition(workflowStep)

        then:
        results == []
    }

    @SuppressWarnings('UnnecessaryGetter')
    //mock of get method
    void "findMatchingJobErrorDefinition, when JobErrorDefinitions available for bean, then log the found cases and return matching definitions"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        LogWithIdentifier logWithIdentifierNoDefinitionFound = new LogWithIdentifier('noDefinition', 'some not matching text')
        LogWithIdentifier logWithIdentifierDefinitionFound = new LogWithIdentifier('definition', 'matching text: doMatch')

        AbstractRestartHandlerLogService sourceTypeNotMatch = Mock(AbstractRestartHandlerLogService)
        AbstractRestartHandlerLogService sourceTypeMatchNoLogs = Mock(AbstractRestartHandlerLogService)
        AbstractRestartHandlerLogService sourceTypeMatchWithLogs = Mock(AbstractRestartHandlerLogService)

        createWorkflowJobErrorDefinition([
                sourceType     : WorkflowJobErrorDefinition.SourceType.CLUSTER_JOB,
                jobBeanName    : workflowStep.beanName,
                errorExpression: 'any expression',
        ])
        createWorkflowJobErrorDefinition([
                sourceType     : WorkflowJobErrorDefinition.SourceType.MESSAGE,
                jobBeanName    : workflowStep.beanName,
                errorExpression: 'some not matching expression',
        ])
        WorkflowJobErrorDefinition definitionMatch = createWorkflowJobErrorDefinition([
                sourceType     : WorkflowJobErrorDefinition.SourceType.MESSAGE,
                jobBeanName    : workflowStep.beanName,
                errorExpression: 'doMatch',
        ])

        service.restartHandlerLogServices = [
                sourceTypeNotMatch,
                sourceTypeMatchNoLogs,
                sourceTypeMatchWithLogs,
        ]

        service.logService = Mock(LogService) {
            1 * addSimpleLogEntry(workflowStep) { String message -> message.contains('since no JobErrorDefinition found') }
            1 * addSimpleLogEntry(workflowStep) { String message -> message.contains('since no logs') }
            1 * addSimpleLogEntry(workflowStep) { String message -> message.contains('since no matching JobErrorDefinition could be found') }
            0 * _
        }

        when:
        List<JobErrorDefinitionWithLogWithIdentifier> results = service.findMatchingJobErrorDefinition(workflowStep)

        then:
        results.size() == 1
        results[0].logWithIdentifier == logWithIdentifierDefinitionFound
        results[0].errorDefinition == definitionMatch

        1 * sourceTypeNotMatch.getSourceType() >> WorkflowJobErrorDefinition.SourceType.WES_RUN_LOG
        0 * sourceTypeNotMatch.createLogsWithIdentifier(workflowStep)
        0 * sourceTypeNotMatch._

        1 * sourceTypeMatchNoLogs.getSourceType() >> WorkflowJobErrorDefinition.SourceType.CLUSTER_JOB
        1 * sourceTypeMatchNoLogs.createLogsWithIdentifier(workflowStep) >> []
        0 * sourceTypeMatchNoLogs._

        1 * sourceTypeMatchWithLogs.getSourceType() >> WorkflowJobErrorDefinition.SourceType.MESSAGE
        1 * sourceTypeMatchWithLogs.createLogsWithIdentifier(workflowStep) >> [
                logWithIdentifierNoDefinitionFound,
                logWithIdentifierDefinitionFound,
        ]
        0 * sourceTypeMatchWithLogs._
    }
}
