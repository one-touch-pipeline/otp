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

import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.json.JSONObject

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.project.Project
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

    Project project

    String workDirectory

    List<ExternalWorkflowConfigFragment> configs

    String combinedConfig

    ProcessingPriority priority

    State state = State.LEGACY

    WorkflowRun restartedFrom

    SkippedMessage skippedMessage

    List<WorkflowStep> workflowSteps

    Workflow workflow

    String displayName

    /**
     * Flag to indicate, whether restarting a job can cause problems.
     *
     * Sometimes jobs needs to do an action, which can not repeated without risking of inconsistency.
     * Example of such an action is sending cluster jobs. Since if a job fail after the cluster job was submitted and before OTP has saved the id, a restart
     * would cause sending the cluster job again, which then run on the same files and cause invalid files.
     *
     * To avoid that, this flag is introduced to mark, that a job starts with such an action and has not finished yet.
     * Since on errors, the transaction is roll-backed, it is necessary to to the change in a separate transaction. Therefore the service method
     * {@link WorkflowRunService#markJobAsNotRestartableInSeparateTransaction(WorkflowRun)} is written. Change back can be done directly in the same action,
     * since that should be persistent with all database changes.
     *
     * If a job do multiple things, it should try to do repeatable action first and start with the not repeatable action as late as possible to minimize
     * the time a job is in this state.
     *
     * If the flag is true, the job system won't restart that job, so only restart of the workflow is possible.
     */
    boolean jobCanBeRestarted = true

    static hasMany = [
            configs      : ExternalWorkflowConfigFragment,
            workflowSteps: WorkflowStep,
    ]

    static constraints = {
        workDirectory nullable: true
        combinedConfig validator: {
            validateCombinedConfig(it)
        }
        restartedFrom nullable: true, validator: { val, obj ->
            if (val && val.workflow != obj.workflow) {
                return 'workflowRun.restartedFrom.workflow.differ'
            }
        }
        skippedMessage nullable: true
        comment nullable: true
        displayName blank: false, nullable: false
    }

    static mapping = {
        combinedConfig type: "text"
        priority index: 'workflow_run_priority_idx'
        state index: 'workflow_run_state_idx'
        comment cascade: "all-delete-orphan"
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

    WorkflowRun getOriginalRestartedFrom() {
        WorkflowRun run = restartedFrom
        while (run?.restartedFrom) {
            run = run.restartedFrom
        }
        return run
    }

    int getRestartCount() {
        int count = 0
        WorkflowRun run = this
        while (run.restartedFrom) {
            run = run.restartedFrom
            count++
        }
        return count
    }

    Realm getRealm() {
        return project.realm
    }

    @SuppressWarnings("Instanceof")
    static boolean validateCombinedConfig(String s) {
        try {
            return JSON.parse(s) instanceof JSONObject
        } catch (ConverterException ignored) {
            return false
        }
    }
}
