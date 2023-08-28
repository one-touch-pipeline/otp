/*
 * Copyright 2011-2023 The OTP authors
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

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Transactional
class WesRunService {

    /**
     * returns all Run of weskit currently checked by OTP
     */
    @CompileDynamic
    List<WesRun> monitoredRuns() {
        return WesRun.findAllByState(WesRun.MonitorState.CHECKING)
    }

    @CompileDynamic
    List<WesRun> allByWorkflowStep(WorkflowStep workflowStep) {
        return WesRun.findAllByWorkflowStep(workflowStep)
    }

    /**
     * Store the WesRun into database.
     * The run state will be set as WesRun.MonitorState.CHECKING
     *
     * @param workflowStep current workflow step
     * @param wesIdentifier ID of the wes run
     * @param subPath last part of the path
     */
    void saveWorkflowRun(WorkflowStep workflowStep, String wesIdentifier, String subPath) {
        WesRun wesRun = new WesRun(
                workflowStep: workflowStep,
                wesIdentifier: wesIdentifier,
                subPath: subPath, // only the last level of the path
                state: WesRun.MonitorState.CHECKING)
        wesRun.save(flush: true)
    }
}
