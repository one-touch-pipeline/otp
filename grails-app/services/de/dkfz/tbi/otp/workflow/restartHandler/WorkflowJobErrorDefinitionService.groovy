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

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Service to find matching {@WorkflowJobErrorDefinition} of available logs for {@link WorkflowStep}.
 *
 * Should only useed by {@link AutoRestartHandlerService}.
 */
@Transactional
class WorkflowJobErrorDefinitionService {

    LogService logService

    List<AbstractRestartHandlerLogService> restartHandlerLogServices

    //needed for mocking
    @SuppressWarnings('UnnecessaryGetter')
    List<JobErrorDefinitionWithLogWithIdentifier> findMatchingJobErrorDefinition(WorkflowStep workflowStep) {
        List<JobErrorDefinitionWithLogWithIdentifier> definitions = []

        List<WorkflowJobErrorDefinition> errorDefinitions = WorkflowJobErrorDefinition.findAllByJobBeanName(workflowStep.beanName)
        if (!errorDefinitions) {
            logService.addSimpleLogEntry(workflowStep, "skip, since no JobErrorDefinition found for ${workflowStep.beanName}")
            return []
        }

        restartHandlerLogServices.each { AbstractRestartHandlerLogService restartHandlerLogService ->
            //for mocking, getSourceType() needs to be used instead of sourceType
            WorkflowJobErrorDefinition.SourceType sourceType = restartHandlerLogService.getSourceType()

            List<WorkflowJobErrorDefinition> errorDefinitionsOfType = errorDefinitions.findAll { WorkflowJobErrorDefinition errorDefinition ->
                errorDefinition.sourceType == sourceType
            }
            if (!errorDefinitionsOfType) {
                //for mocking of AbstractRestartHandlerLogService, getClass() needs to be used instead of class
                logService.addSimpleLogEntry(workflowStep, "skip logService ${restartHandlerLogService.getClass().name}, " +
                        "since no JobErrorDefinition found of type ${sourceType} for bean ${workflowStep.beanName}")
                return
            }
            Collection<LogWithIdentifier> logs = restartHandlerLogService.createLogsWithIdentifier(workflowStep)
            if (!logs) {
                //for mocking of AbstractRestartHandlerLogService, getClass() needs to be used instead of class
                logService.addSimpleLogEntry(workflowStep, "skip logService ${restartHandlerLogService.getClass().name}, " +
                        "since no logs found for type ${sourceType} and bean ${workflowStep.beanName}")
                return
            }
            logs.each { LogWithIdentifier logWithIdentifier ->
                List<WorkflowJobErrorDefinition> matchingErrorDefinitions = errorDefinitionsOfType.findAll { WorkflowJobErrorDefinition errorDefinition ->
                    logWithIdentifier.log =~ errorDefinition.errorExpression
                }
                if (!matchingErrorDefinitions) {
                    logService.addSimpleLogEntry(workflowStep, "skip log ${logWithIdentifier.identifier}, " +
                            "since no matching JobErrorDefinition could be found")
                    return
                }
                matchingErrorDefinitions.each { WorkflowJobErrorDefinition errorDefinition ->
                    definitions << new JobErrorDefinitionWithLogWithIdentifier(errorDefinition, logWithIdentifier)
                }
            }
        }
        return definitions
    }
}
