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
package de.dkfz.tbi.otp.workflowExecution.wes

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@ManagedEntity
class WesRun implements Entity {

    /**
     * enum about the state of checking
     */
    enum MonitorState {

        /**
         * Indicate, that the current WesRun is in the monitor
         */
        CHECKING,

        /**
         * Indicate, that the monitoring has finished
         */
        FINISHED
    }

    /**
     * workflow step this run belongs to
     */
    WorkflowStep workflowStep

    /**
     * Holds the identifier of the run
     */
    String wesIdentifier

    /**
     * A sub directory in the work directory to be able to handle multiple weskit calls within one workflow run
     */
    String subPath

    /**
     * holds the monitor state
     */
    MonitorState state

    /**
     * reference to the log of the instance
     */
    WesRunLog wesRunLog // nullable, since filled by monitor

    static Closure constraints = {
        wesRunLog nullable: true
    }

    static belongsTo = [
            workflowStep: WorkflowStep,
    ]

    static Closure mapping = {
        workflowStep index: 'wes_run_workflow_step_idx'
        wesRunLog index: 'wes_run_wes_run_log_idx'
        state index: 'wes_run_state_idx'
        wesIdentifier index: 'wes_run_wes_identifier_idx'
    }

    @Override
    String toString() {
        return "WesRun(${id}): ${wesIdentifier} " +
            "with work directory ${subPath} " +
            "in monitoring state ${state} " +
            "linking to workflowStep ${workflowStep} and " +
            "wesRunLog ${wesRunLog}"
    }
}
