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
package de.dkfz.tbi.otp.workflowExecution

import grails.gorm.transactions.Transactional

@Transactional
class WorkflowService {
    JobService jobService

    void createRestartedWorkflow(WorkflowStep step, boolean startDirectly = true) {
        assert step
        assert step.workflowRun.state == WorkflowRun.State.FAILED
        WorkflowRun run = new WorkflowRun(
                workflow: step.workflowRun.workflow,
        ).save(flush: true)

        Map<String, WorkflowArtefact> newArtefacts = step.workflowRun.outputArtefacts.collectEntries { String role, WorkflowArtefact oldArtefact ->
            WorkflowArtefact newArtefact = new WorkflowArtefact(
                    state: WorkflowArtefact.State.PLANNED_OR_RUNNING,
                    producedBy: run,
            ).save(flush: true)

            WorkflowRunInputArtefact.findAllByWorkflowArtefact(oldArtefact).each { WorkflowRunInputArtefact workflowRunInputArtefact ->
                workflowRunInputArtefact.workflowArtefact = newArtefact
                workflowRunInputArtefact.save(flush: true)
            }

            oldArtefact.state = WorkflowArtefact.State.FAILED
            oldArtefact.save(flush: true)

            return [(role): newArtefact]
        } as Map<String, WorkflowArtefact>
        run.outputArtefacts = newArtefacts
        run.save(flush: true)

        step.workflowRun.state = WorkflowRun.State.FAILED_FINAL
        step.workflowRun.save(flush: true)

        if (startDirectly) {
            jobService.createNextJob(run)
        }
    }
}