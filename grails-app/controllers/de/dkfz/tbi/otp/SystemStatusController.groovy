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
package de.dkfz.tbi.otp

import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.workflowExecution.*

@Secured("hasRole('ROLE_ADMIN')")
class SystemStatusController implements CheckAndCall {

    WorkflowSystemService workflowSystemService
    WorkflowService workflowService
    WorkflowRunOverviewService workflowRunOverviewService

    static allowedMethods = [
            index              : "GET",
            startWorkflowSystem: "POST",
            stopWorkflowSystem : "POST",
            enableWorkflow     : "POST",
            disableWorkflow    : "POST",
    ]

    def index() {
        List<Workflow> workflows = Workflow.findAllByDeprecatedDateIsNull().sort { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString()) }
        return [
                runs                  : workflowRunOverviewService.getNumberOfRunsPerWorkflowAndState(),
                workflowSystem        : workflowSystemService.enabled,
                workflows             : workflows,
                numberEnabledWorkflows: workflows.findAll { it.enabled }.size(),
        ]
    }


    def startWorkflowSystem() {
        checkErrorAndCallMethodWithFlashMessage(null, 'systemStatus.workflowSystem.started') {
            workflowSystemService.startWorkflowSystem()
        }
        redirect action: "index"
    }

    def stopWorkflowSystem() {
        checkErrorAndCallMethodWithFlashMessage(null, 'systemStatus.workflowSystem.stopped') {
            workflowSystemService.stopWorkflowSystem()
        }
        redirect action: "index"
    }

    def enableWorkflow(Workflow workflow) {
        checkErrorAndCallMethodWithFlashMessage(null, 'systemStatus.workflows.enabled') {
            workflowService.enableWorkflow(workflow)
        }
        redirect action: "index"
    }

    def disableWorkflow(Workflow workflow) {
        checkErrorAndCallMethodWithFlashMessage(null, 'systemStatus.workflows.disabled') {
            workflowService.disableWorkflow(workflow)
        }
        redirect action: "index"
    }
}
