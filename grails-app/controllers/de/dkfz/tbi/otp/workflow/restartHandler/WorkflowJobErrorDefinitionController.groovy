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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.util.Holders
import grails.validation.Validateable
import groovy.transform.ToString

import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.jobs.Job

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Secured("hasRole('ROLE_OPERATOR')")
class WorkflowJobErrorDefinitionController extends AbstractGeneralDomainPropertyUpdateController {

    WorkflowJobErrorDefinitionService workflowJobErrorDefinitionService

    static allowedMethods = [
            index            : "GET",
            create           : "GET",
            createObject     : "POST",
            delete           : "POST",
            updateActionField: "POST",
    ]

    @Override
    protected Class getEntityClass() {
        return WorkflowJobErrorDefinition
    }

    def index() {
        return [
                definitions : workflowJobErrorDefinitionService.listAll(),
                jobBeanNames: Holders.applicationContext.getBeanNamesForType(Job).sort(),
        ]
    }

    def create() {
        WorkflowJobErrorDefinitionCreateCommand cmd = flash.cmd
        return [
                cmd         : cmd,
                sourceTypes : WorkflowJobErrorDefinition.SourceType.values(),
                actions     : WorkflowJobErrorDefinition.Action.values(),
                jobBeanNames: Holders.applicationContext.getBeanNamesForType(Job).sort(),
        ]
    }

    def createObject(WorkflowJobErrorDefinitionCreateCommand cmd) {
        flash.cmd = cmd
        boolean success = checkErrorAndCallMethodWithFlashMessage(cmd, "workflowJobErrorDefinition.create") {
            workflowJobErrorDefinitionService.create(cmd)
            return true
        }
        redirect(action: success ? "index" : "create")
    }

    def delete(WorkflowJobErrorDefinitionDeleteCommand cmd) {
        checkErrorAndCallMethodWithFlashMessage(cmd, "workflowJobErrorDefinition.delete") {
            workflowJobErrorDefinitionService.delete(cmd.workflowJobErrorDefinition)
        }
        redirect(action: "index")
    }

    /**
     * Update a single value property of a domain.
     * Its the controller method for {@link UpdateDomainPropertyService#updateProperty(Class, Long, String, String)}.
     * The domain class is fetch about the callback {@link #getEntityClass()}. The other values are provided from {@link UpdateDomainPropertyCommand}.
     *
     * @param cmd command object for the GUI parameters, see  {@link UpdateDomainPropertyCommand}
     * @return a JSON providing information about success or failure.
     */
    JSON updateActionField(WorkflowJobErrorDefinitionUpdateActionCommand cmd) {
        return checkErrorAndCallMethodWithExtendedMessagesAndJsonRendering(cmd) {
            WorkflowJobErrorDefinition definition = workflowJobErrorDefinitionService.updateAction(cmd.workflowJobErrorDefinition, cmd.value)
            return ['restartBean': definition.beanToRestart]
        }
    }

}

@ToString(includeNames = true)
class WorkflowJobErrorDefinitionCreateCommand implements Validateable {
    String name
    String jobBeanName
    WorkflowJobErrorDefinition.SourceType sourceType
    WorkflowJobErrorDefinition.Action restartAction
    String errorExpression
    int allowRestartingCount
    String beanToRestart
    String mailText

    static constraints = {
        errorExpression(validator: { val, obj ->
            try {
                Pattern.compile(val)
            }
            catch (PatternSyntaxException e) {
                return 'workflowJobErrorDefinition.errorExpression.invalid'
            }
            return true
        })
        mailText nullable: true
        allowRestartingCount min: 0
        jobBeanName blank: false
        name blank: false
        beanToRestart nullable: true, validator: { val, obj ->
            if (obj.restartAction == WorkflowJobErrorDefinition.Action.RESTART_JOB) {
                if (!obj.beanToRestart) {
                    return 'workflowJobErrorDefinition.beanToRestart.null'
                }
            } else {
                if (obj.beanToRestart) {
                    return 'workflowJobErrorDefinition.beanToRestart.notNull'
                }
            }
        }
    }
}

class WorkflowJobErrorDefinitionDeleteCommand implements Validateable {
    WorkflowJobErrorDefinition workflowJobErrorDefinition
}

class WorkflowJobErrorDefinitionUpdateActionCommand implements Validateable {
    WorkflowJobErrorDefinition workflowJobErrorDefinition
    WorkflowJobErrorDefinition.Action value
}
