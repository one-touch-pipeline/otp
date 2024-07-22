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

import groovy.transform.CompileDynamic

class WorkflowStepService {

    /**
     * If the {@link WorkflowStep} was restart, then take the job before the restarted. Otherwise use the job run before this job.
     *
     * Example with 4 jobs, calling the method on job D:
     * jobs: job A -> job B -> job C -> Job D
     *
     * Case 1: Job D was not restarted
     * --> return Job C
     *
     * Case 2: job D is the restarted from job B
     * --> return Job A
     *
     * @return the previous running {@link WorkflowStep}, ignoring restarted ones.
     */
    WorkflowStep getPreviousRunningWorkflowStep(WorkflowStep workflowStep) {
        return (workflowStep.restartedFrom ?: workflowStep).previous
    }

    @CompileDynamic
    List<WorkflowStep> runningWorkflowSteps() {
        return WorkflowStep.withCriteria {
            eq('state', WorkflowStep.State.RUNNING)
            workflowRun {
                eq('state', WorkflowRun.State.RUNNING_OTP)
            }
        } as List<WorkflowStep>
    }
}
