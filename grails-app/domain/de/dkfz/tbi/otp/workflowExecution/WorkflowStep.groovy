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
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowLog

class WorkflowStep implements Commentable, Entity {

    enum State {
        CREATED,
        RUNNING,
        SKIPPED,
        SUCCESS,
        FAILED,
    }

    WorkflowRun workflowRun

    String beanName

    String wesIdentifier

    State state

    WorkflowError workflowError

    WorkflowStep previous

    WorkflowStep restartedFrom

    boolean obsolete = false

    Set<ClusterJob> clusterJobs

    List<WorkflowLog> logs

    static hasMany = [
            clusterJobs: ClusterJob,
            logs: WorkflowLog,
    ]

    static constraints = {
        wesIdentifier nullable: true
        workflowError nullable: true, validator: { val, obj ->
            if ((obj.state == State.FAILED) ^ (val != null)) {
                return false
            }
        }
        previous nullable: true
        restartedFrom nullable: true
        comment nullable: true
    }

    static mapping = {
        comment cascade: "all-delete-orphan"
    }

    WorkflowStep getNext() {
        return CollectionUtils.atMostOneElement(findAllByPrevious(this))
    }

    String getWesData() {
        return null
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
}
