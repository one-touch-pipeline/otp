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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog
import de.dkfz.tbi.otp.workflowExecution.wes.WesRun

@ManagedEntity
class WorkflowStep implements Commentable, Entity {

    /**
     * @see {@link WorkflowRun.State} for descriptions
     */
    enum State {
        CREATED,
        RUNNING,
        OMITTED,
        SUCCESS,
        FAILED,
    }

    WorkflowRun workflowRun

    String beanName

    State state

    WorkflowError workflowError

    WorkflowStep previous

    WorkflowStep restartedFrom

    boolean obsolete = false

    Set<ClusterJob> clusterJobs = [] as Set

    Set<WesRun> wesRuns = [] as Set

    static belongsTo = [
            workflowRun: WorkflowRun
    ]

    static hasMany = [
            clusterJobs: ClusterJob,
            wesRuns: WesRun,
    ]

    static Closure constraints = {
        workflowError nullable: true, validator: { val, obj ->
            if ((obj.state == State.FAILED) ^ (val != null)) {
                return false
            }
        }
        previous nullable: true, validator: { val, obj ->
            if (val && val.workflowRun != obj.workflowRun) {
                return 'workflowStep.previous.workflowRun.differ'
            }
        }
        restartedFrom nullable: true, unique: true, validator: { val, obj ->
            if (val && val.workflowRun != obj.workflowRun) {
                return 'workflowStep.restartedFrom.workflowRun.differ'
            }
            if (val && val.beanName != obj.beanName) {
                return 'workflowStep.restartedFrom.beanName.differ'
            }
        }

        comment nullable: true
    }

    static Closure mapping = {
        comment cascade: "all-delete-orphan"
        workflowRun index: 'workflow_step_workflow_run_idx'
        state index: 'workflow_step_state_idx'
        workflowError index: 'workflow_step_workflow_error_idx'
        previous index: 'workflow_step_previous_idx'
        restartedFrom index: 'workflow_step_restarted_from_idx'
    }

    WorkflowStep getNext() {
        return CollectionUtils.atMostOneElement(findAllByPrevious(this))
    }

    WorkflowStep getOriginalRestartedFrom() {
        WorkflowStep step = restartedFrom
        while (step?.restartedFrom) {
            step = step.restartedFrom
        }
        return step
    }

    int getRestartCount() {
        int count = 0
        WorkflowStep step = this
        while (step.restartedFrom) {
            step = step.restartedFrom
            count++
        }
        return count
    }

    ProcessingPriority getPriority() {
        return workflowRun.priority
    }

    /**
     * @Deprecated Use {@link WorkflowLogService#findAllByWorkflowStepInCorrectOrder(WorkflowStep)} instead
     */
    @Deprecated
    List<WorkflowLog> getLogs() {
        return WorkflowLog.findAllByWorkflowStep(this).sort {
            it.dateCreated
        }
    }

    Realm getRealm() {
        return workflowRun.realm
    }

    String displayInfo() {
        return "workflowStep ${id} ${beanName} in ${state} for ${workflowRun.displayInfo()}"
    }

    @Override
    String toString() {
        return "workflowStep ${id} ${beanName} in ${state}"
    }
}
