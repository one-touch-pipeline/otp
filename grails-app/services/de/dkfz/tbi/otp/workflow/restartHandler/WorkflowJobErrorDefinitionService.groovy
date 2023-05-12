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
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition.Action
import de.dkfz.tbi.otp.workflow.restartHandler.logging.RestartHandlerLogService
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@CompileDynamic
@Transactional
class WorkflowJobErrorDefinitionService {

    LogService logService

    List<RestartHandlerLogService> restartHandlerLogServices

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<WorkflowJobErrorDefinition> listAll() {
        return WorkflowJobErrorDefinition.list().sort {
            it.name
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    WorkflowJobErrorDefinition create(WorkflowJobErrorDefinitionCreateCommand cmd) {
        return new WorkflowJobErrorDefinition([
                name                : cmd.name,
                jobBeanName         : cmd.jobBeanName,
                sourceType          : cmd.sourceType,
                action              : cmd.restartAction,
                errorExpression     : cmd.errorExpression,
                allowRestartingCount: cmd.allowRestartingCount,
                beanToRestart       : cmd.beanToRestart ?: null,
                mailText            : cmd.mailText,
        ]).save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void delete(WorkflowJobErrorDefinition workflowJobErrorDefinition) {
        workflowJobErrorDefinition.delete(flush: true)
    }

    /**
     * Change the action and handle the depending property beanToRestart.
     *
     * When the new action is {@link Action#RESTART_JOB} and {@link WorkflowJobErrorDefinition#beanToRestart} is null, then set it to the value of
     * {@link WorkflowJobErrorDefinition#jobBeanName}.
     * When the new action is not {@link Action#RESTART_JOB} and {@link WorkflowJobErrorDefinition#beanToRestart} is not null, then set it to the value null.
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    WorkflowJobErrorDefinition updateAction(WorkflowJobErrorDefinition workflowJobErrorDefinition, Action action) {
        workflowJobErrorDefinition.action = action
        if (action == Action.RESTART_JOB && !workflowJobErrorDefinition.beanToRestart) {
            workflowJobErrorDefinition.beanToRestart = workflowJobErrorDefinition.jobBeanName
        }
        if (action != Action.RESTART_JOB && workflowJobErrorDefinition.beanToRestart) {
            workflowJobErrorDefinition.beanToRestart = null
        }
        return workflowJobErrorDefinition.save(flush: true)
    }

    /**
     * Find matching {@WorkflowJobErrorDefinition} of available logs for {@link WorkflowStep}.
     *
     * Should only used by {@link AutoRestartHandlerService}.
     */
    //UnnecessaryGetter is needed for mocking
    @SuppressWarnings('UnnecessaryGetter')
    List<JobErrorDefinitionWithLogWithIdentifier> findMatchingJobErrorDefinition(WorkflowStep workflowStep) {
        List<JobErrorDefinitionWithLogWithIdentifier> definitions = []

        List<WorkflowJobErrorDefinition> errorDefinitions = WorkflowJobErrorDefinition.findAllByJobBeanName(workflowStep.beanName)
        if (!errorDefinitions) {
            logService.addSimpleLogEntry(workflowStep, "skip, since no JobErrorDefinition found for ${workflowStep.beanName}")
            return []
        }

        restartHandlerLogServices.each { RestartHandlerLogService restartHandlerLogService ->
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
