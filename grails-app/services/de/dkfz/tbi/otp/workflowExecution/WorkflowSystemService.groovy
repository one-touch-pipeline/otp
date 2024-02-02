/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.config.OptionProblem
import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException

@Transactional
class WorkflowSystemService {

    JobService jobService
    PropertiesValidationService propertiesValidationService
    WorkflowStepService workflowStepService
    WorkflowBeanNameService workflowBeanNameService

    private boolean firstStart = true

    private boolean enabled = false

    void startWorkflowSystem() {
        if (enabled) {
            log.info("job system already started, ignoring")
            return
        }

        List<OptionProblem> validationResult = propertiesValidationService.validateProcessingOptions()
        if (!validationResult.isEmpty()) {
            throw new WorkflowException(validationResult.join("\n"))
        }

        List<String> implementedWorkflowsMissingBeanName =
                workflowBeanNameService.findWorkflowBeanNamesNotSet()
        if (!implementedWorkflowsMissingBeanName.empty) {
            throw new WorkflowException(
                "Following implemented workflows don't have a bean name stored in the database: ${implementedWorkflowsMissingBeanName.join(', ')}")
        }

        if (firstStart) {
            List<WorkflowStep> runningSteps = workflowStepService.runningWorkflowSteps()
            runningSteps.each {
                jobService.createRestartedJobAfterSystemRestart(it)
            }
            firstStart = false
            log.info("job system handle first start")
        }
        enabled = true
        log.info("job system started")
    }

    void stopWorkflowSystem() {
        if (!enabled) {
            log.info("job system already stopped, ignoring")
            return
        }
        enabled = false
        log.info("job system stopped")
    }

    boolean hasRunAfterStart() {
        return !firstStart
    }

    boolean isEnabled() {
        return enabled
    }
}
