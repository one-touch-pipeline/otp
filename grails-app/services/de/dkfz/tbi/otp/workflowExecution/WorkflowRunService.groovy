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

class WorkflowRunService {

    private final static String WAITING_WORKFLOW_QUERY = """
        from
            WorkflowRun wr
        where
            wr.state = '${WorkflowRun.State.PENDING}'
            and wr.priority in (
                select
                    pp
                from
                    ProcessingPriority pp
                where
                    pp.allowedParallelWorkflowRuns > :workflowCount
            )
            and not exists (
                from
                    WorkflowRunInputArtefact wia
                where
                    wia.workflowRun = wr
                    and wia.workflowArtefact.state != '${WorkflowArtefact.State.SUCCESS}'
            )
        order by
            wr.priority.priority desc,
            wr.workflow.priority desc,
            wr.dateCreated
        """

    final static List<WorkflowRun.State> STATES_COUNTING_AS_RUNNING = [
            WorkflowRun.State.RUNNING,
            WorkflowRun.State.WAITING_ON_SYSTEM,
    ].asImmutable()

    int countOfRunningWorkflows() {
        return WorkflowRun.countByStateInList(STATES_COUNTING_AS_RUNNING)
    }

    WorkflowRun nextWaitingWorkflow(int workflowCount) {
        return WorkflowRun.find(WAITING_WORKFLOW_QUERY, [
                workflowCount: workflowCount,
        ])
    }

    WorkflowRun createWorkflowRun(Workflow workflow, ProcessingPriority priority, String workDirectory, List<ExternalWorkflowConfigFragment> configs = []) {
        return new WorkflowRun([
                workDirectory : workDirectory,
                state         : WorkflowRun.State.PENDING,
                configs       : configs,
                combinedConfig: null,
                priority      : priority,
                restartedFrom : null,
                skippedMessage: null,
                workflowSteps : [],
                workflow      : workflow,
        ]).save(flush: false)
    }
}
