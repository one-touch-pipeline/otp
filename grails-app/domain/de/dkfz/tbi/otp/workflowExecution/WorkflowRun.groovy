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

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.utils.Entity

class WorkflowRun implements Commentable, Entity {

    enum State {
        //unfinished
        PENDING,
        WAITING_ON_SYSTEM,
        WAITING_ON_USER,
        RUNNING,
        FAILED,
        //finished
        SKIPPED,
        SUCCESS,
        FAILED_FINAL,
        KILLED,
        //other
        LEGACY,
    }

    String workDirectory

    List<ExternalWorkflowConfigFragment> configs

    String combinedConfig

    ProcessingPriority priority

    State state = State.LEGACY

    WorkflowRun restartedFrom

    SkippedMessage skippedMessage

    List<WorkflowStep> workflowSteps

    Workflow workflow

    static hasMany = [
        configs: ExternalWorkflowConfigFragment,
        workflowSteps: WorkflowStep,
    ]

    static constraints = {
        workDirectory nullable: true
        combinedConfig nullable: true
        restartedFrom nullable: true
        skippedMessage nullable: true
        comment nullable: true
    }

    static mapping = {
        combinedConfig type: "text"
        priority index: 'workflow_run_priority_idx'
        state index: 'workflow_run_state_idx'
    }

    Map<String, WorkflowArtefact> getInputArtefacts() {
        WorkflowRunInputArtefact.findAllByWorkflowRun(this).collectEntries {
            [it.role, it.workflowArtefact]
        }
    }

    Map<String, WorkflowArtefact> getOutputArtefacts() {
        WorkflowArtefact.findAllByProducedBy(this).collectEntries {
            [it.outputRole, it]
        }
    }
}
